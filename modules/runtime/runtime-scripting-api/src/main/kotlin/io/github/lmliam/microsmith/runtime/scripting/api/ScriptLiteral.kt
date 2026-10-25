package io.github.lmliam.microsmith.runtime.scripting.api

sealed interface ScriptLiteral {
    data class StringValue(val value: String) : ScriptLiteral

    data class IntValue(val value: Int) : ScriptLiteral

    data class BooleanValue(val value: Boolean) : ScriptLiteral
}
