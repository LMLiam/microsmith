package me.liam.microsmith.cli.command

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal data class InitCommand(
    val projectRoot: Path = Path.of("."),
    val diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT,
    val verbose: Boolean = false,
    val nonInteractive: Boolean = false,
    val assumeYes: Boolean = false,
    val force: Boolean = false,
    val skipIdeHelper: Boolean = false,
) : CliCommand
