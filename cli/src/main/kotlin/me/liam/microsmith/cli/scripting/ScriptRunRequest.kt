package me.liam.microsmith.cli.scripting

import java.nio.file.Path

internal data class ScriptRunRequest(
    val script: Path,
    val outputDir: Path,
    val variables: Map<String, String>,
    val flags: Set<String>
)
