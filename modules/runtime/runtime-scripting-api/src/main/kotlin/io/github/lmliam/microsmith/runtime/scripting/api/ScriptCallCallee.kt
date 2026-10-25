package io.github.lmliam.microsmith.runtime.scripting.api
sealed interface ScriptCallCallee {
    data class Named(val name: String) : ScriptCallCallee

    data class StringLiteral(val value: String) : ScriptCallCallee

    data class IntLiteral(val value: Int) : ScriptCallCallee

    data object Other : ScriptCallCallee
}
