package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
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
                    successFinalizer.complete(
                        evaluationResult = result.value,
                        scriptContext = scriptContext,
                        warnings = formattedReports,
                        cacheHit = cacheHit,
                        elapsedMillis = elapsedMillis,
                    )
                }
        }
    }
}
