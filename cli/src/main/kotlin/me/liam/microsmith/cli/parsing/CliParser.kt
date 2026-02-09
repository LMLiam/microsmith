package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Path

private const val RUN_COMMAND = "run"
private const val OUTPUT_OPTION = "--out"
private const val SCRIPT_EXTENSION = ".microsmith.kts"
private val HELP_COMMANDS = setOf("--help", "-h", "help")

internal fun parseCliArgs(args: List<String>): CliCommand {
    val command = args.firstOrNull()
    return when {
        command == null || command in HELP_COMMANDS -> HelpCommand
        command != RUN_COMMAND -> ErrorCommand("Unknown command '$command'.")
        else -> parseRunCommand(args)
    }
}

private fun parseRunCommand(args: List<String>): CliCommand {
    val (script, scriptError) = parseScriptArg(args.getOrNull(1))
    return when {
        scriptError != null -> ErrorCommand(scriptError)
        script == null -> ErrorCommand("Missing <script.microsmith.kts> argument for run command.")
        else -> parseOutputCommand(script, args, startIndex = 2)
    }
}

private fun parseScriptArg(scriptArg: String?): Pair<Path?, String?> {
    val result =
        when {
            scriptArg == null || scriptArg.startsWith("--") ->
                null to "Missing <script.microsmith.kts> argument for run command."

            !scriptArg.endsWith(SCRIPT_EXTENSION) ->
                null to "Script file must use the .microsmith.kts extension."

            else -> Path.of(scriptArg) to null
        }
    return result
}

private fun parseOutputCommand(
    script: Path,
    args: List<String>,
    startIndex: Int
): CliCommand {
    val (outputDir, outputError) = parseOutputDir(args, startIndex)
    return when {
        outputError != null -> ErrorCommand(outputError)
        outputDir == null -> ErrorCommand("Missing required --out <output-dir> option.")
        else -> RunCommand(script = script, outputDir = outputDir)
    }
}

private fun parseOutputDir(
    args: List<String>,
    startIndex: Int
): Pair<Path?, String?> {
    var outputDir: Path? = null
    var index = startIndex
    var error: String? = null

    while (index < args.size && error == null) {
        val token = args[index]
        if (token == OUTPUT_OPTION) {
            val value = args.getOrNull(index + 1)
            error =
                when {
                    value == null || value.startsWith("--") -> "Missing value for --out option."
                    outputDir != null -> "--out option may only be specified once."
                    else -> null
                }
            if (error == null && value != null) {
                outputDir = Path.of(value)
                index += 2
            }
        } else {
            error = "Unknown option '$token'."
        }

        if (token != OUTPUT_OPTION || error != null) {
            index += 1
        }
    }

    return outputDir to error
}
