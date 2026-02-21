package me.liam.microsmith.runtime.scripting.model

enum class ScriptIsolationMode(val cliValue: String) {
    CLASSLOADER("classloader"),
    PROCESS("process"),
    ;

    companion object {
        fun fromCliValue(value: String): ScriptIsolationMode? = entries.firstOrNull { mode ->
            mode.cliValue == value.trim().lowercase()
        }
    }
}
