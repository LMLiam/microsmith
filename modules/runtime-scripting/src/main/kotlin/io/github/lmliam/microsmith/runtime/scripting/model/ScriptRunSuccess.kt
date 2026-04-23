package io.github.lmliam.microsmith.runtime.scripting.model

import java.nio.file.Path

data class ScriptRunSuccess(
    val warnings: List<String>,
    val cacheHit: Boolean,
    val elapsedMillis: Long,
    val generatedRoots: List<Path> = emptyList(),
) :
    ScriptRunResult
