package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat

internal data class ParsedDoctorOptions(
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val error: String?,
)
