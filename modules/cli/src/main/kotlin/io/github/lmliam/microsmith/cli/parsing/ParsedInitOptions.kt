package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal data class ParsedInitOptions(
    val projectRoot: Path,
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val force: Boolean,
    val skipIdeHelper: Boolean,
    val error: String?,
)
