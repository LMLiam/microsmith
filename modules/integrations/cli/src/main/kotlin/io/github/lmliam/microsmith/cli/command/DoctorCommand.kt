package io.github.lmliam.microsmith.cli.command

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat

internal data class DoctorCommand(
    val diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT,
    val verbose: Boolean = false,
) : CliCommand
