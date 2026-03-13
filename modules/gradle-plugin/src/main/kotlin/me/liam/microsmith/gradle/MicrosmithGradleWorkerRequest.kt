package me.liam.microsmith.gradle

import java.nio.file.Path

internal data class MicrosmithGradleWorkerRequest(
    val scriptPath: Path,
    val outputPath: Path,
    val cacheDirectory: Path,
    val variables: Map<String, String>,
    val flags: Set<String>,
    val pluginClasspath: List<Path>,
)
