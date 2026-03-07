package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat

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
