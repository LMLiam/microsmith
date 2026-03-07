package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal data class ParsedIdeOptions(
    val projectRoot: Path,
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val error: String?,
)
