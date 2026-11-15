package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.composition.ScriptRunGraph
import io.github.lmliam.microsmith.runtime.scripting.host.isolation.process.ProcessIsolatedScriptExecutor
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext

@Inject
internal class ScriptRunExecutor(
    private val runGraphFactory: ScriptRunGraph.Factory,
    private val processExecutor: ProcessIsolatedScriptExecutor,
) {
    suspend fun execute(request: ScriptRunRequest): ScriptRunResult =
        when (request.isolationMode) {
            ScriptIsolationMode.CLASSLOADER -> executeInClassLoader(request)

            ScriptIsolationMode.PROCESS ->
                processExecutor.execute(request = request, scriptPath = request.script, outputPath = request.outputDir)
        }

    private suspend fun executeInClassLoader(request: ScriptRunRequest): ScriptRunResult = runCatching {
        val runGraph = runGraphFactory.create(request, currentCoroutineContext().job)
        try {
            runGraph.run.execute()
        } finally {
            withContext(NonCancellable) {
                try {
                    runGraph.classLoader.close()
                } finally {
                    runGraph.leftoverPluginThreads.stop()
                }
            }
        }
    }
        .getOrElse { exception ->
            if (exception is CancellationException) throw exception
            val message = exception.message ?: exception::class.simpleName ?: "unknown error"
            ScriptRunFailure.withMessages(
                messages = listOf("Unhandled script host failure: $message"),
                type = ScriptFailureType.HOST,
            )
        }
}
