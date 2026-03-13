package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

internal class ScriptRunExecutor(
    cacheDirectory: Path,
    private val classloaderRunner: ClassloaderScriptRunner = ClassloaderScriptRunner(cacheDirectory),
    private val processExecutor: ProcessIsolatedScriptExecutor = ProcessIsolatedScriptExecutor(cacheDirectory),
) {
    fun execute(request: ScriptRunRequest, scriptPath: Path, outputPath: Path): ScriptRunResult {
        val normalizedRequest = request.withNormalizedPluginClasspath()
        return when (normalizedRequest.isolationMode) {
            ScriptIsolationMode.CLASSLOADER ->
                classloaderRunner.execute(
                    request = normalizedRequest,
                    scriptPath = scriptPath,
                    outputPath = outputPath,
                )

            ScriptIsolationMode.PROCESS ->
                processExecutor.execute(
                    request = normalizedRequest,
                    scriptPath = scriptPath,
                    outputPath = outputPath,
                )
        }
    }
}

private fun ScriptRunRequest.withNormalizedPluginClasspath(): ScriptRunRequest =
    copy(pluginClasspath = pluginClasspath.map { it.toAbsolutePath().normalize() })
