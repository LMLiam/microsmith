package me.liam.microsmith.gradle

import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path

internal class MicrosmithGradleWorkerLauncher(
    private val requestCodec: MicrosmithGradleWorkerRequestCodec = MicrosmithGradleWorkerRequestCodec(),
    private val resultCodec: MicrosmithGradleWorkerResultCodec = MicrosmithGradleWorkerResultCodec(),
) {
    fun execute(
        request: MicrosmithGradleWorkerRequest,
        workDirectory: Path,
        runtimeClasspath: List<Path>,
    ): MicrosmithGradleWorkerResult {
        val workspace = Files.createDirectories(workDirectory.resolve("microsmith-worker"))
        val requestFile = workspace.resolve("request.properties")
        val resultFile = workspace.resolve("result.properties")
        requestCodec.write(requestFile, request)
        val outcome = execute(runtimeClasspath, requestFile, resultFile)
        return outcome.parsedResult ?: throw GradleException(formatFailure(outcome, resultFile))
    }

    private fun execute(
        runtimeClasspath: List<Path>,
        requestFile: Path,
        resultFile: Path,
    ): MicrosmithGradleWorkerExecutionOutcome {
        val process =
            ProcessBuilder(
                buildCommand(runtimeClasspath, requestFile, resultFile),
            ).redirectErrorStream(true)
                .start()
        val processOutput = process.inputStream.bufferedReader().use { reader -> reader.readText().trim() }
        val exitCode = process.waitFor()
        val parsedResult = runCatching { resultCodec.read(resultFile) }.getOrNull()
        return MicrosmithGradleWorkerExecutionOutcome(exitCode, processOutput, parsedResult)
    }

    private fun buildCommand(runtimeClasspath: List<Path>, requestFile: Path, resultFile: Path): List<String> = listOf(
        resolveJavaCommand().toString(),
        "-cp",
        runtimeClasspath.joinToString(FilePathSeparator.value),
        MicrosmithGradleWorkerMain::class.java.name,
        requestFile.toString(),
        resultFile.toString(),
    )

    private fun resolveJavaCommand(): Path {
        val javaHome = Path.of(System.getProperty("java.home"))
        val executableName = if (isWindows()) "java.exe" else "java"
        val executable = javaHome.resolve("bin").resolve(executableName)
        require(Files.exists(executable) && Files.isRegularFile(executable)) {
            "Could not locate Java executable at '$executable' for Microsmith Gradle worker."
        }
        return executable
    }

    private fun formatFailure(outcome: MicrosmithGradleWorkerExecutionOutcome, resultFile: Path): String = buildString {
        appendLine("Microsmith Gradle worker did not produce a readable result.")
        appendLine("Worker exit code: ${outcome.exitCode}")
        appendLine("Expected result file: $resultFile")
        if (outcome.processOutput.isNotBlank()) {
            appendLine("Worker output:")
            appendLine(outcome.processOutput)
        }
    }.trimEnd()

    private fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
}
