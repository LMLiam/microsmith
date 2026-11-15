package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.cache.MicrosmithScriptCache
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.host.configuration.ScriptHostConfigurationFactory
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path
import kotlin.io.path.createDirectories

@Inject
internal class ScriptRun(
    private val request: ScriptRunRequest,
    private val cacheDirectory: Path,
    private val classLoader: ScriptRunClassLoader,
    private val cache: MicrosmithScriptCache,
    private val scriptContext: MicrosmithScriptContext,
    private val hostConfigurationFactory: ScriptHostConfigurationFactory,
    private val evaluator: ScriptEvaluator,
    private val resultMapper: ScriptRunResultMapper,
) {
    fun execute(): ScriptRunResult = classLoader.withContextClassLoader {
        cacheDirectory.createDirectories()
        val runtimeHostConfiguration = hostConfigurationFactory.create()
        val (result, elapsedMillis) =
            evaluator.evaluate(
                scriptPath = request.script,
                runtimeHostConfiguration = runtimeHostConfiguration,
                scriptContext = scriptContext,
                pluginClasspath = request.pluginClasspath,
            )
        cache.recordWarnings(result.reports)
        resultMapper.toRunResult(
            result = result,
            elapsedMillis = elapsedMillis,
            scriptContext = scriptContext,
            cacheHit = cache.retrievedScripts > 0,
            replayedWarnings = cache.replayedWarnings,
        )
    }
}
