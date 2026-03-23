package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import java.nio.file.Path

internal data class ParsedRunOptions(
    val outputDir: Path,
    val variables: Map<String, String>,
    val flags: Set<String>,
    val plugins: Set<String>,
    val pluginJars: Set<Path>,
    val offline: Boolean,
    val repositoryOverride: String?,
    val isolationMode: ScriptIsolationMode,
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val eventLog: Path?,
    val error: String?,
)
