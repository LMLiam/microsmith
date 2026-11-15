package io.github.lmliam.microsmith.runtime.scripting.model

public data class ScriptRunFailure(
    public val diagnostics: List<ScriptRunDiagnostic>,
    public val type: ScriptFailureType = ScriptFailureType.HOST,
) : ScriptRunResult {
    public companion object {
        public fun withMessages(
            messages: List<String>,
            type: ScriptFailureType = ScriptFailureType.HOST,
        ): ScriptRunFailure =
            ScriptRunFailure(diagnostics = messages.map { message -> ScriptRunDiagnostic(message) }, type = type)
    }
}
