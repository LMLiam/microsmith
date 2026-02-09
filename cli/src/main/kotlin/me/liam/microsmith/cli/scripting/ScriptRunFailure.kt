package me.liam.microsmith.cli.scripting

internal data class ScriptRunFailure(
    val diagnostics: List<String>
) : ScriptRunResult
