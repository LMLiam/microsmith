package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.command.IdeRefreshCommand
import java.nio.file.Path

internal fun parseIdeCommand(args: List<String>): CliCommand = when (val subcommand = args.getOrNull(1)) {
    null, in HELP_COMMANDS -> ErrorCommand("Missing <refresh|doctor> subcommand for ide command.")
    IDE_REFRESH_SUBCOMMAND -> parseIdeRefreshCommand(args = args, startIndex = 2)
    IDE_DOCTOR_SUBCOMMAND -> parseIdeDoctorCommand(args = args, startIndex = 2)
    else -> ErrorCommand("Unknown ide subcommand '$subcommand'. Expected 'refresh' or 'doctor'.")
}

private fun parseIdeRefreshCommand(args: List<String>, startIndex: Int): CliCommand {
    val parsed = parseIdeOptions(args = args, startIndex = startIndex)
    return if (parsed.error != null) {
        ErrorCommand(parsed.error)
    } else {
        IdeRefreshCommand(
            projectRoot = parsed.projectRoot,
            diagnosticsFormat = parsed.diagnosticsFormat,
            verbose = parsed.verbose,
        )
    }
}

private fun parseIdeDoctorCommand(args: List<String>, startIndex: Int): CliCommand {
    val parsed = parseIdeOptions(args = args, startIndex = startIndex)
    return if (parsed.error != null) {
        ErrorCommand(parsed.error)
    } else {
        IdeDoctorCommand(
            projectRoot = parsed.projectRoot,
            diagnosticsFormat = parsed.diagnosticsFormat,
            verbose = parsed.verbose,
        )
    }
}

private fun parseIdeOptions(args: List<String>, startIndex: Int): ParsedIdeOptions {
    val diagnosticOptions = DiagnosticOptionsState()
    var projectRoot = Path.of(".")
    var projectRootSpecified = false
    var error: String? = null
    var index = startIndex

    while (index < args.size && error == null && diagnosticOptions.error == null) {
        val parsedToken =
            when (val token = args[index]) {
                DIAGNOSTICS_OPTION -> ParsedToken(
                    index + diagnosticOptions.consumeDiagnostics(args = args, index = index),
                )

                VERBOSE_OPTION ->
                    ParsedToken(
                        index + diagnosticOptions.consumeVerbose(),
                    )

                REPO_ROOT_OPTION ->
                    parseRepoRootOption(
                        args = args,
                        index = index,
                        alreadySpecified = projectRootSpecified,
                    ) { parsedProjectRoot ->
                        projectRoot = parsedProjectRoot
                        projectRootSpecified = true
                    }

                else -> ParsedToken(nextIndex = index, error = "Unknown option '$token'.")
            }
        if (parsedToken.error != null) {
            error = parsedToken.error
        }
        if (parsedToken.nextIndex <= index) {
            break
        }
        index = parsedToken.nextIndex
    }

    return ParsedIdeOptions(
        projectRoot = projectRoot,
        diagnosticsFormat = diagnosticOptions.diagnosticsFormat,
        verbose = diagnosticOptions.verbose,
        error = diagnosticOptions.error ?: error,
    )
}
