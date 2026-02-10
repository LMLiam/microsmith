package me.liam.microsmith.runtime.scripting

data class ScriptRunSuccess(
    val warnings: List<String>,
    val cacheHit: Boolean,
    val elapsedMillis: Long
) : ScriptRunResult

