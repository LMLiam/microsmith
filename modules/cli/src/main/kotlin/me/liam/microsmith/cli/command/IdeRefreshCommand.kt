package me.liam.microsmith.cli.command

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal data class IdeRefreshCommand(
    val projectRoot: Path = Path.of("."),
    val diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT,
    val verbose: Boolean = false,
) : CliCommand
