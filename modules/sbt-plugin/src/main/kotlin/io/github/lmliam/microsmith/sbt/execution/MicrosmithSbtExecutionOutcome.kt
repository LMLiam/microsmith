package io.github.lmliam.microsmith.sbt.execution

import java.nio.file.Path

data class MicrosmithSbtExecutionOutcome(
    val outputDirectory: Path,
    val warnings: List<String>,
    val cacheHit: Boolean,
    val elapsedMillis: Long,
    val generatedRoots: List<Path> = emptyList(),
)
