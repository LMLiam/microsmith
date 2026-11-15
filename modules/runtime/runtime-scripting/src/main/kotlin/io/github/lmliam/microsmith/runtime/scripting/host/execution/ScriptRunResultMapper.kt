package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.host.diagnostics.ScriptDiagnosticsFormatter
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics

@Inject
internal class ScriptRunResultMapper(
    private val successFinalizer: ScriptEvaluationSuccessFinalizer,
    private val diagnosticsFormatter: ScriptDiagnosticsFormatter,
) {
    fun toRunResult(
        result: ResultWithDiagnostics<EvaluationResult>,
        elapsedMillis: Long,
        scriptContext: MicrosmithScriptContext,
        cacheHit: Boolean,
        replayedWarnings: List<String>,
    ): ScriptRunResult {
        val formattedReports = replayedWarnings + diagnosticsFormatter.format(result.reports)
        val hasErrors = diagnosticsFormatter.containsErrors(formattedReports)

        return when (result) {
            is ResultWithDiagnostics.Failure ->
                ScriptRunFailure.withMessages(
                    messages = formattedReports.ifEmpty { listOf("Script compilation failed.") },
                    type = ScriptFailureType.COMPILATION,
                )

            is ResultWithDiagnostics.Success ->
                if (hasErrors) {
                    ScriptRunFailure.withMessages(
                        messages = formattedReports,
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
