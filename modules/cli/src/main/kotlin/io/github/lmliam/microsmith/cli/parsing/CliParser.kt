package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.command.CliCommand
import io.github.lmliam.microsmith.cli.command.ErrorCommand
import io.github.lmliam.microsmith.cli.command.HelpCommand
import io.github.lmliam.microsmith.cli.command.VersionCommand

internal fun parseCliArgs(args: List<String>): CliCommand = when (val command = args.firstOrNull()) {
    null, in HELP_COMMANDS -> HelpCommand
    in VERSION_COMMANDS -> parseVersionCommand(args)
    INIT_COMMAND -> parseInitCommand(args)
    RUN_COMMAND -> parseRunCommand(args)
    DOCTOR_COMMAND -> parseDoctorCommand(args)
    IDE_COMMAND -> parseIdeCommand(args)
    else -> ErrorCommand("Unknown command '$command'.")
}

private fun parseVersionCommand(args: List<String>): CliCommand =
    if (args.size > 1) ErrorCommand("The --version command does not accept additional arguments.") else VersionCommand
