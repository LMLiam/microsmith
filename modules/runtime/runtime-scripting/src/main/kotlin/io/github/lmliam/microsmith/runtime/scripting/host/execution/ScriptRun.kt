package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.cache.MicrosmithScriptCache
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.host.configuration.ScriptHostConfigurationFactory
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.createDirectories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

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
    suspend fun execute(): ScriptRunResult {
        val outcome = CompletableDeferred<ScriptRunResult>()
        thread(name = SCRIPT_THREAD_NAME, contextClassLoader = classLoader.loader) {
            outcome.completeWith(runCatching { evaluate() })
        }

        val result =
            try {
                outcome.await()
            } catch (cancellation: CancellationException) {
                withContext(NonCancellable) { outcome.join() }
                throw cancellation
            }
        currentCoroutineContext().ensureActive()
        return result
    }

    private fun evaluate(): ScriptRunResult {
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
        return resultMapper.toRunResult(
            result = result,
            elapsedMillis = elapsedMillis,
            scriptContext = scriptContext,
            cacheHit = cache.retrievedScripts > 0,
            replayedWarnings = cache.replayedWarnings,
        )
    }
}

private const val SCRIPT_THREAD_NAME = "microsmith-script-run"
