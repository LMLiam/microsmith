package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import dev.zacsweers.metro.Inject
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Inject
internal class JvmProcessIsolationWorkerLauncher(private val resultCodec: ProcessIsolationResultCodec) :
    ProcessIsolationWorkerLauncher {
    override fun execute(requestFile: Path, resultFile: Path): ProcessIsolationExecutionOutcome {
        val process = ProcessBuilder(buildCommand(requestFile, resultFile)).redirectErrorStream(true).start()
        val processOutput = process.inputStream.bufferedReader().use { reader -> reader.readText().trim() }
        val exitCode = process.waitFor()
        val parsedResult = runCatching { resultCodec.read(resultFile) }.getOrNull()
        return ProcessIsolationExecutionOutcome(
            exitCode = exitCode,
            processOutput = processOutput,
            parsedResult = parsedResult,
        )
    }

    private fun buildCommand(requestFile: Path, resultFile: Path): List<String> {
        val classpath = System.getProperty("java.class.path")
        require(!classpath.isNullOrBlank()) {
            "Process isolation requires a non-empty java.class.path system property."
        }
        return listOf(
            resolveJavaCommand().toString(),
            "-cp",
            classpath,
            ProcessIsolatedScriptMain::class.java.name,
            requestFile.toString(),
            resultFile.toString(),
        )
    }

    private fun resolveJavaCommand(): Path {
        val javaHome = Path.of(System.getProperty("java.home"))
        val executableName = if (isWindows()) "java.exe" else "java"
        val executable = javaHome.resolve("bin").resolve(executableName)
        require(executable.exists() && executable.isRegularFile()) {
            "Could not locate Java executable at '$executable' for process isolation mode."
        }
        return executable
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("windows")
}
