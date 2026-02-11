package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.cache.MicrosmithScriptCache
import me.liam.microsmith.runtime.scripting.cache.RuntimeClasspathFingerprint
import me.liam.microsmith.runtime.scripting.model.ScriptIsolationMode
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Files
import java.nio.file.Path

internal class ScriptRunExecutor(
    private val cacheDirectory: Path,
) {
    fun execute(request: ScriptRunRequest, scriptPath: Path, outputPath: Path): ScriptRunResult {
        val normalizedPluginClasspath = request.pluginClasspath.map { it.toAbsolutePath().normalize() }
        return when (request.isolationMode) {
            ScriptIsolationMode.CLASSLOADER ->
                executeInClassloader(
                    request = request.copy(pluginClasspath = normalizedPluginClasspath),
                    scriptPath = scriptPath,
                    outputPath = outputPath,
                )

            ScriptIsolationMode.PROCESS ->
                ProcessIsolatedScriptExecutor(cacheDirectory)
                    .execute(
                        request = request.copy(pluginClasspath = normalizedPluginClasspath),
                        scriptPath = scriptPath,
                        outputPath = outputPath,
                    )
        }
    }

    private fun executeInClassloader(request: ScriptRunRequest, scriptPath: Path, outputPath: Path): ScriptRunResult {
        val pluginClasspath = request.pluginClasspath.map { it.toAbsolutePath().normalize() }
        return PluginClassLoaderScope.withPluginClassLoader(pluginClasspath) { runtimeClassLoader ->
            runCatching {
                val pluginClasspathFingerprint = RuntimeClasspathFingerprint.calculate(pluginClasspath)
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
                        pluginClasspath = pluginClasspath,
                    )
                ScriptRunResultMapper.toRunResult(result, elapsedMillis, scriptContext, cache)
            }.getOrElse { exception ->
                val message = exception.message ?: exception::class.simpleName ?: "unknown error"
                ScriptRunFailure(
                    diagnostics = listOf("Unhandled script host failure: $message"),
                )
            }
        }
    }
}
