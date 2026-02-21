package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.runtime.scripting.cache.MicrosmithScriptCache
import me.liam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics

internal object ScriptRunResultMapper {
    fun toRunResult(
        result: ResultWithDiagnostics<EvaluationResult>,
        elapsedMillis: Long,
        scriptContext: MicrosmithScriptContext,
        cache: MicrosmithScriptCache,
    ): ScriptRunResult {
        val formattedReports = ScriptDiagnosticsFormatter.format(result.reports)
        val hasErrors = ScriptDiagnosticsFormatter.containsErrors(formattedReports)

        return when (result) {
            is ResultWithDiagnostics.Failure ->
                ScriptRunFailure(
                    diagnostics = formattedReports.ifEmpty { listOf("Script compilation failed.") },
                    type = ScriptFailureType.COMPILATION,
                )

            is ResultWithDiagnostics.Success ->
                if (hasErrors) {
                    ScriptRunFailure(
                        diagnostics = formattedReports,
                        type = ScriptFailureType.COMPILATION,
                    )
                } else {
                    finalizeSuccess(
                        evaluationResult = result.value,
                        scriptContext = scriptContext,
                        warnings = formattedReports,
                        cacheHit = cache.retrievedScripts > 0,
                        elapsedMillis = elapsedMillis,
                    )
                }
        }
    }

    private fun finalizeSuccess(
        evaluationResult: EvaluationResult,
        scriptContext: MicrosmithScriptContext,
        warnings: List<String>,
        cacheHit: Boolean,
        elapsedMillis: Long,
    ): ScriptRunResult {
        val generationResult = runCatching { ensureModelGenerated(evaluationResult, scriptContext) }
        return generationResult.fold(
            onSuccess = {
                ScriptRunSuccess(
                    warnings = warnings,
                    cacheHit = cacheHit,
                    elapsedMillis = elapsedMillis,
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

    private fun ensureModelGenerated(evaluationResult: EvaluationResult, scriptContext: MicrosmithScriptContext) {
        when (val returnValue = evaluationResult.returnValue) {
            is ResultValue.Error -> rethrow(returnValue.error)

            is ResultValue.Value -> {
                val returnedModel = returnValue.value as? MicrosmithModel
                when {
                    scriptContext.emittedAny() -> Unit
                    returnedModel != null -> scriptContext.emit(returnedModel)
                    else -> modelRequiredFailure()
                }
            }

            is ResultValue.Unit,
            ResultValue.NotEvaluated,
            -> {
                if (!scriptContext.emittedAny()) {
                    modelRequiredFailure()
                }
            }
        }
    }
}

private fun rethrow(error: Throwable): Nothing = throw error

private fun modelRequiredFailure(): Nothing = error(MODEL_REQUIRED_ERROR_MESSAGE)

private const val MODEL_REQUIRED_ERROR_MESSAGE =
    "Script must either return MicrosmithModel or call emit(model)/generate(model)."
