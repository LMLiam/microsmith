package me.liam.microsmith.runtime.scripting

import java.nio.file.Path

data class ScriptRunRequest(
    val script: Path,
    val outputDir: Path,
    val variables: Map<String, String>,
    val flags: Set<String>,
    val pluginClasspath: List<Path> = emptyList()
)

