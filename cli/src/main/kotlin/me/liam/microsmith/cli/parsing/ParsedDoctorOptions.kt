package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat

internal data class ParsedDoctorOptions(
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val error: String?,
)
