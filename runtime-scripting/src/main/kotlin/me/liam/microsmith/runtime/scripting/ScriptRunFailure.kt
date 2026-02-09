package me.liam.microsmith.runtime.scripting

data class ScriptRunFailure(
    val diagnostics: List<String>
) : ScriptRunResult

