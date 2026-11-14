package io.github.lmliam.microsmith.runtime.scripting.host.execution

import io.github.lmliam.microsmith.gen.errors.GenerationResolutionFailedException
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunDiagnostic
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import kotlin.script.experimental.api.EvaluationResult

internal class ScriptEvaluationSuccessFinalizer(
    private val modelEmitter: ScriptEvaluationModelEmitter = ScriptEvaluationModelEmitter(),
) {
    fun complete(
        evaluationResult: EvaluationResult,
        scriptContext: MicrosmithScriptContext,
        warnings: List<String>,
        cacheHit: Boolean,
        elapsedMillis: Long,
    ): ScriptRunResult = runCatching {
        modelEmitter.ensureGenerated(evaluationResult, scriptContext)
    }
        .fold(
            onSuccess = {
                ScriptRunSuccess(
                    warnings = warnings,
                    cacheHit = cacheHit,
                    elapsedMillis = elapsedMillis,
                    generatedRoots = scriptContext.generatedRoots(),
                )
            },
            onFailure = { error -> failureOf(error, warnings) },
        )

    private fun failureOf(error: Throwable, warnings: List<String>): ScriptRunFailure {
        val resolutionFailure = error.causeChain().filterIsInstance<GenerationResolutionFailedException>().firstOrNull()
        if (resolutionFailure != null) {
            return ScriptRunFailure(
                diagnostics =
                    warnings.map { warning -> ScriptRunDiagnostic(warning) } +
                        resolutionFailure.diagnostics.map { diagnostic ->
                            ScriptRunDiagnostic(message = diagnostic.message, code = diagnostic.code)
                        },
                type = ScriptFailureType.RESOLUTION,
            )
        }
        val message = error.message ?: error::class.simpleName ?: "unknown error"
        return ScriptRunFailure.withMessages(
            messages = warnings + listOf("Script evaluation failed: $message"),
            type = ScriptFailureType.EVALUATION,
        )
    }
}

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(this) { current -> current.cause?.takeIf { cause -> cause !== current } }
