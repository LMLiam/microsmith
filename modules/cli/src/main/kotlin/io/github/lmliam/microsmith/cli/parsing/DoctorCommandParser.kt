package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.command.CliCommand
import io.github.lmliam.microsmith.cli.command.DoctorCommand
import io.github.lmliam.microsmith.cli.command.ErrorCommand

internal fun parseDoctorCommand(args: List<String>): CliCommand {
    val parsed = parseDoctorOptions(args = args, startIndex = 1)
    parsed.error?.let { return ErrorCommand(it) }
    return DoctorCommand(
        diagnosticsFormat = parsed.diagnosticsFormat,
        verbose = parsed.verbose,
    )
}

private fun parseDoctorOptions(args: List<String>, startIndex: Int): ParsedDoctorOptions {
    val state = DiagnosticOptionsState()
    var index = startIndex

    while (index < args.size && state.error == null) {
        val consumed =
            when (val token = args[index]) {
                DIAGNOSTICS_OPTION -> state.consumeDiagnostics(args = args, index = index)

                VERBOSE_OPTION -> state.consumeVerbose()

                else -> {
                    state.consumeUnknownOption(token)
                    0
                }
            }
        if (consumed <= 0) {
            break
        }
        index += consumed
    }

    return ParsedDoctorOptions(
        diagnosticsFormat = state.diagnosticsFormat,
        verbose = state.verbose,
        error = state.error,
    )
}
