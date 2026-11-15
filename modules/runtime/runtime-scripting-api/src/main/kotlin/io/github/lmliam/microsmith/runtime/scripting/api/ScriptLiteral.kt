package io.github.lmliam.microsmith.runtime.scripting.api

public sealed interface ScriptLiteral {
    public data class StringValue(public val value: String) : ScriptLiteral

    public data class IntValue(public val value: Int) : ScriptLiteral

    public data class BooleanValue(public val value: Boolean) : ScriptLiteral
}
