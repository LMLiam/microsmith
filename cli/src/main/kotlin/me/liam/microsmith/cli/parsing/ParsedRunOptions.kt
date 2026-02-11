package me.liam.microsmith.cli.parsing

import java.nio.file.Path

internal data class ParsedRunOptions(
    val outputDir: Path?,
    val variables: Map<String, String>,
    val flags: Set<String>,
    val plugins: Set<String>,
    val pluginJars: Set<Path>,
    val offline: Boolean,
    val repositoryOverride: String?,
    val error: String?,
)
