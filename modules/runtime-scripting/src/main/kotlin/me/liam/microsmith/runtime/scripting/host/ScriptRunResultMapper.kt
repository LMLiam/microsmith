package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics

internal object ScriptRunResultMapper {
    private val successFinalizer = ScriptEvaluationSuccessFinalizer()

    fun toRunResult(
        result: ResultWithDiagnostics<EvaluationResult>,
        elapsedMillis: Long,
        scriptContext: MicrosmithScriptContext,
        cacheHit: Boolean,
    ): ScriptRunResult {
        val formattedReports = ScriptDiagnosticsFormatter.format(result.reports)
        val hasErrors = ScriptDiagnosticsFormatter.containsErrors(formattedReports)

        if (result is ResultWithDiagnostics.Failure) {
            return ScriptRunFailure(
                diagnostics = formattedReports.ifEmpty { listOf("Script compilation failed.") },
                type = ScriptFailureType.COMPILATION,
            )
        }

        if (hasErrors) {
            return ScriptRunFailure(
                diagnostics = formattedReports,
                type = ScriptFailureType.COMPILATION,
            )
        }

        val successResult = result as ResultWithDiagnostics.Success
        return successFinalizer.complete(
            evaluationResult = successResult.value,
            scriptContext = scriptContext,
            warnings = formattedReports,
            cacheHit = cacheHit,
            elapsedMillis = elapsedMillis,
        )
    }
}
