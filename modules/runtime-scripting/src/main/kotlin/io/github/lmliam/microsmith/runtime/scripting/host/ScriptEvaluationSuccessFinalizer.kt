package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
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
    }.fold(
        onSuccess = {
            ScriptRunSuccess(
                warnings = warnings,
                cacheHit = cacheHit,
                elapsedMillis = elapsedMillis,
                generatedRoots = scriptContext.generatedRoots(),
            )
        },
        onFailure = { error ->
            val message = error.message ?: error::class.simpleName ?: "unknown error"
            ScriptRunFailure(
                diagnostics = warnings + listOf("Script evaluation failed: $message"),
                type = ScriptFailureType.EVALUATION,
            )
        },
    )
}
