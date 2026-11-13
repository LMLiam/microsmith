package io.github.lmliam.microsmith.runtime.scripting.model

data class ScriptRunFailure(
    val diagnostics: List<ScriptRunDiagnostic>,
    val type: ScriptFailureType = ScriptFailureType.HOST,
) : ScriptRunResult {
    companion object {
        fun withMessages(messages: List<String>, type: ScriptFailureType = ScriptFailureType.HOST): ScriptRunFailure =
            ScriptRunFailure(diagnostics = messages.map { message -> ScriptRunDiagnostic(message) }, type = type)
    }
}
