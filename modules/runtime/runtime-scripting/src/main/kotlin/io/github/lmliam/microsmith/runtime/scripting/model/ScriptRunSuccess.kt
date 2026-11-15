package io.github.lmliam.microsmith.runtime.scripting.model

import java.nio.file.Path

public data class ScriptRunSuccess(
    public val warnings: List<String>,
    public val cacheHit: Boolean,
    public val elapsedMillis: Long,
    public val generatedRoots: List<Path> = emptyList(),
) : ScriptRunResult
