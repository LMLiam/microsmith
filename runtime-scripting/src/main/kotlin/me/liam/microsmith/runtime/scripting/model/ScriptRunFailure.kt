package me.liam.microsmith.runtime.scripting.model

data class ScriptRunFailure(
    val diagnostics: List<String>
) : ScriptRunResult

