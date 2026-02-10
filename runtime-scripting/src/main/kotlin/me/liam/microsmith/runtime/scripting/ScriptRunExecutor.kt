package me.liam.microsmith.runtime.scripting

import java.nio.file.Files
import java.nio.file.Path

internal class ScriptRunExecutor(
    private val cacheDirectory: Path
) {
    fun execute(
        request: ScriptRunRequest,
        scriptPath: Path,
        outputPath: Path
    ): ScriptRunResult {
        val pluginClasspath = request.pluginClasspath.map { it.toAbsolutePath().normalize() }
        return PluginClassLoaderScope.withPluginClassLoader(pluginClasspath) { runtimeClassLoader ->
            runCatching {
                Files.createDirectories(cacheDirectory)
                val cache = MicrosmithScriptCache(cacheDirectory)
                val runtimeHostConfiguration =
                    ScriptHostConfigurationFactory.create(
                        cache = cache,
                        runtimeClassLoader = runtimeClassLoader
                    )
                val scriptContext = ScriptContextFactory.create(outputPath, request)
                val (result, elapsedMillis) =
                    ScriptEvaluator.evaluate(
                        scriptPath = scriptPath,
                        runtimeHostConfiguration = runtimeHostConfiguration,
                        scriptContext = scriptContext,
                        pluginClasspath = pluginClasspath
                    )
                ScriptRunResultMapper.toRunResult(result, elapsedMillis, scriptContext, cache)
            }.getOrElse { exception ->
                val message = exception.message ?: exception::class.simpleName ?: "unknown error"
                ScriptRunFailure(
                    diagnostics = listOf("Unhandled script host failure: $message")
                )
            }
        }
    }
}
