package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.HELP_TEXT
import io.github.lmliam.microsmith.cli.command.ErrorCommand
import io.github.lmliam.microsmith.cli.diagnostics.CliFailureCode
import io.github.lmliam.microsmith.cli.diagnostics.DiagnosticFormat

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
