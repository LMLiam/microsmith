package me.liam.microsmith.cli.command

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import me.liam.microsmith.runtime.scripting.model.ScriptIsolationMode
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
    val auditLog: Path? = null,
) : CliCommand
