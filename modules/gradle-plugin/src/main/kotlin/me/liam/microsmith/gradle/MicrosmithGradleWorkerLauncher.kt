package me.liam.microsmith.gradle

import org.gradle.api.GradleException
import java.nio.file.Files
import java.nio.file.Path

internal class MicrosmithGradleWorkerLauncher(
    private val requestCodec: MicrosmithGradleWorkerRequestCodec = MicrosmithGradleWorkerRequestCodec(),
    private val resultCodec: MicrosmithGradleWorkerResultCodec = MicrosmithGradleWorkerResultCodec(),
    private val processExecutor: MicrosmithGradleWorkerProcessExecutor =
        DefaultMicrosmithGradleWorkerProcessExecutor(),
) {
    fun execute(
        request: MicrosmithGradleWorkerRequest,
        workDirectory: Path,
        runtimeClasspath: List<Path>,
    ): MicrosmithGradleWorkerResult {
        val workspace = createWorkspace(workDirectory)
        val requestFile = workspace.resolve(WORKER_REQUEST_FILE_NAME)
        val resultFile = workspace.resolve(WORKER_RESULT_FILE_NAME)
        requestCodec.write(requestFile, request)
        val outcome = executeProcess(runtimeClasspath, requestFile, resultFile)
        return outcome.parsedResult ?: throw GradleException(formatFailure(outcome, resultFile))
    }

    private fun createWorkspace(workDirectory: Path): Path {
        Files.createDirectories(workDirectory)
        return Files.createTempDirectory(workDirectory, WORKER_DIRECTORY_PREFIX)
    }

    private fun executeProcess(
        runtimeClasspath: List<Path>,
        requestFile: Path,
        resultFile: Path,
    ): MicrosmithGradleWorkerExecutionOutcome {
        val processOutcome =
            processExecutor.execute(
                buildCommand(runtimeClasspath, requestFile, resultFile),
            )
        val parsedResult = resultFile.takeIf(Files::isRegularFile)?.let { file ->
            runCatching { resultCodec.read(file) }.getOrNull()
        }
        return MicrosmithGradleWorkerExecutionOutcome(
            exitCode = processOutcome.exitCode,
            processOutput = processOutcome.processOutput,
            parsedResult = parsedResult,
        )
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

private const val WORKER_DIRECTORY_PREFIX = "microsmith-worker-"
private const val WORKER_REQUEST_FILE_NAME = "request.properties"
private const val WORKER_RESULT_FILE_NAME = "result.properties"
