package io.github.lmliam.microsmith.runtime.scripting.model

data class ScriptRunSuccess(val warnings: List<String>, val cacheHit: Boolean, val elapsedMillis: Long) :
    ScriptRunResult
