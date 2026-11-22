package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@Inject
internal class JvmProcessIsolationWorkerLauncher(
    private val resultCodec: ProcessIsolationResultCodec,
    private val environment: Environment,
    private val fileSystem: FileSystem,
) : ProcessIsolationWorkerLauncher {
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
        val classpath = environment.property(CLASS_PATH_PROPERTY)
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
        val javaHome =
            fileSystem.getPath(
                checkNotNull(environment.property(JAVA_HOME_PROPERTY)) { "$JAVA_HOME_PROPERTY is not set" },
            )
        val executableName = if (isWindows()) "java.exe" else "java"
        val executable = javaHome.resolve("bin").resolve(executableName)
        require(executable.exists() && executable.isRegularFile()) {
            "Could not locate Java executable at '$executable' for process isolation mode."
        }
        return executable
    }

    private fun isWindows(): Boolean = environment.property(OS_NAME_PROPERTY).orEmpty().lowercase().contains("windows")

    private companion object {
        const val CLASS_PATH_PROPERTY = "java.class.path"
        const val JAVA_HOME_PROPERTY = "java.home"
        const val OS_NAME_PROPERTY = "os.name"
    }
}
