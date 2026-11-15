package io.github.lmliam.microsmith.runtime.scripting.model

public enum class ScriptIsolationMode(public val cliValue: String) {
    CLASSLOADER("classloader"),
    PROCESS("process");

    public companion object {
        public fun fromCliValue(value: String): ScriptIsolationMode? = entries.firstOrNull { mode ->
            mode.cliValue == value.trim().lowercase()
        }
    }
}
