package io.github.lmliam.microsmith.gradle

import java.nio.file.Path

internal data class MicrosmithGradleWorkerSuccess(
    val warnings: List<String>,
    val cacheHit: Boolean,
    val elapsedMillis: Long,
    val generatedRoots: List<Path> = emptyList(),
) : MicrosmithGradleWorkerResult
