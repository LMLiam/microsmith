package me.liam.microsmith.sbt

import java.nio.file.Path

data class MicrosmithSbtExecutionConfiguration(
    val baseDirectory: Path,
    val scriptFile: Path,
    val outputDirectory: Path,
    val cacheDirectory: Path,
    val variables: Map<String, String>,
    val flags: Set<String>,
)
