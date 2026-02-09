package me.liam.microsmith.cli.scripting

internal data class ScriptRunSuccess(
    val warnings: List<String>,
    val cacheHit: Boolean,
    val elapsedMillis: Long
) : ScriptRunResult
