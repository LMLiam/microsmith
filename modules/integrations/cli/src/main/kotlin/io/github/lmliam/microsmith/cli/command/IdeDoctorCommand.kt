package io.github.lmliam.microsmith.cli.command

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal data class IdeDoctorCommand(
    val projectRoot: Path = Path.of("."),
    val diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT,
    val verbose: Boolean = false,
) : CliCommand
