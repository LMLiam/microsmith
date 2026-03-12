package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue

internal class ScriptEvaluationModelEmitter {
    fun ensureGenerated(evaluationResult: EvaluationResult, scriptContext: MicrosmithScriptContext) {
        when (val returnValue = evaluationResult.returnValue) {
            is ResultValue.Error -> rethrow(returnValue.error)
            is ResultValue.Value -> emitReturnedModelIfNeeded(returnValue.value, scriptContext)
            is ResultValue.Unit,
            ResultValue.NotEvaluated,
            -> requireGenerated(scriptContext)
        }
    }

    private fun emitReturnedModelIfNeeded(returnValue: Any?, scriptContext: MicrosmithScriptContext) {
        when {
            scriptContext.emittedAny() -> Unit
            returnValue is MicrosmithModel -> scriptContext.emit(returnValue)
            else -> error(MODEL_REQUIRED_ERROR_MESSAGE)
        }
    }

    private fun requireGenerated(scriptContext: MicrosmithScriptContext) {
        if (!scriptContext.emittedAny()) {
            error(MODEL_REQUIRED_ERROR_MESSAGE)
        }
    }
}

private fun rethrow(error: Throwable): Nothing = throw error

private const val MODEL_REQUIRED_ERROR_MESSAGE =
    "Script must either return MicrosmithModel or call emit(model)/generate(model)."
