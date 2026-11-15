package io.github.lmliam.microsmith.runtime.scripting.api

public sealed interface ScriptCallCallee {
    public data class Named(public val name: String) : ScriptCallCallee

    public data class StringLiteral(public val value: String) : ScriptCallCallee

    public data class IntLiteral(public val value: Int) : ScriptCallCallee

    public data object Other : ScriptCallCallee
}
