package me.liam.microsmith.cli.parsing

import me.liam.microsmith.runtime.scripting.model.ScriptIsolationMode
import java.nio.file.Path

internal data class ParsedRunOptions(
    val outputDir: Path?,
    val variables: Map<String, String>,
    val flags: Set<String>,
    val plugins: Set<String>,
    val pluginJars: Set<Path>,
    val offline: Boolean,
    val repositoryOverride: String?,
    val isolationMode: ScriptIsolationMode,
    val error: String?,
)
