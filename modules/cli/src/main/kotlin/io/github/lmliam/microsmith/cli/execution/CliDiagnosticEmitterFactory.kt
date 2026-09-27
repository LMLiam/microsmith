package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat

internal class CliDiagnosticEmitterFactory(
    private val stdout: (String) -> Unit,
    private val stderr: (String) -> Unit,
) {
    fun create(format: DiagnosticFormat, verbose: Boolean): CliDiagnosticEmitter = CliDiagnosticEmitter(
        format = format,
        verbose = verbose,
        stdout = stdout,
        stderr = stderr,
    )
}
