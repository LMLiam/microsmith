package me.liam.microsmith.cli.command

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat

internal data class DoctorCommand(
    val diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT,
    val verbose: Boolean = false,
) : CliCommand
