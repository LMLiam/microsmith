package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.cache.MicrosmithScriptCache
import me.liam.microsmith.runtime.scripting.cache.RuntimeClasspathFingerprint
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Files
import java.nio.file.Path

internal class ClassloaderScriptRunner(private val cacheDirectory: Path) {
    fun execute(request: ScriptRunRequest, scriptPath: Path, outputPath: Path): ScriptRunResult =
        PluginClassLoaderScope.withPluginClassLoader(request.pluginClasspath) { runtimeClassLoader ->
            runCatching {
                val pluginClasspathFingerprint = RuntimeClasspathFingerprint.calculate(request.pluginClasspath)
                Files.createDirectories(cacheDirectory)
                val cache = MicrosmithScriptCache(cacheDirectory) { listOf(pluginClasspathFingerprint) }
                val runtimeHostConfiguration =
                    ScriptHostConfigurationFactory.create(
                        cache = cache,
                        runtimeClassLoader = runtimeClassLoader,
                    )
                val scriptContext = ScriptContextFactory.create(outputPath, request)
                val (result, elapsedMillis) =
                    ScriptEvaluator.evaluate(
                        scriptPath = scriptPath,
                        runtimeHostConfiguration = runtimeHostConfiguration,
                        scriptContext = scriptContext,
                        pluginClasspath = request.pluginClasspath,
                    )
                ScriptRunResultMapper.toRunResult(
                    result = result,
                    elapsedMillis = elapsedMillis,
                    scriptContext = scriptContext,
                    cacheHit = cache.retrievedScripts > 0,
                )
            }.getOrElse { exception ->
                val message = exception.message ?: exception::class.simpleName ?: "unknown error"
                ScriptRunFailure(
                    diagnostics = listOf("Unhandled script host failure: $message"),
                    type = ScriptFailureType.HOST,
                )
            }
        }
}
