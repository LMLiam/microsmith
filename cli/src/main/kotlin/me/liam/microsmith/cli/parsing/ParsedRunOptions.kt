package me.liam.microsmith.cli.parsing

import java.nio.file.Path

internal data class ParsedRunOptions(
    val outputDir: Path?,
    val variables: Map<String, String>,
    val flags: Set<String>,
    val error: String?
)
