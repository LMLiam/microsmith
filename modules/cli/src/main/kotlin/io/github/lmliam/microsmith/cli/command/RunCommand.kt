package io.github.lmliam.microsmith.cli.command

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import java.nio.file.Path

internal data class RunCommand(
    val script: Path,
    val outputDir: Path,
    val variables: Map<String, String> = emptyMap(),
    val flags: Set<String> = emptySet(),
    val plugins: Set<String> = emptySet(),
    val pluginJars: Set<Path> = emptySet(),
    val offline: Boolean = false,
    val repositoryOverride: String? = null,
    val isolationMode: ScriptIsolationMode = ScriptIsolationMode.CLASSLOADER,
    val diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT,
    val verbose: Boolean = false,
    val eventLog: Path? = null,
) : CliCommand
