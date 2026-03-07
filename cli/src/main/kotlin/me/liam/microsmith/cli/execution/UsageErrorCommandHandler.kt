package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.HELP_TEXT
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat

internal class UsageErrorCommandHandler(
    private val emitterFactory: CliDiagnosticEmitterFactory,
    private val stderr: (String) -> Unit,
) {
    fun execute(command: ErrorCommand): Int {
        val emitter = emitterFactory.create(format = DiagnosticFormat.TEXT, verbose = false)
        emitter.error(CliFailureCode.USAGE_ERROR, command.message)
        stderr("")
        stderr(HELP_TEXT.trimIndent())
        return CliFailureCode.USAGE_ERROR.exitCode
    }
}
