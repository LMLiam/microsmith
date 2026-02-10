package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Path

private const val RUN_COMMAND = "run"
private const val OUTPUT_OPTION = "--out"
private const val VARIABLE_OPTION = "--var"
private const val FLAG_OPTION = "--flag"
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
    val parsedOptions = parseOutputDir(args, startIndex)
    return when {
        parsedOptions.error != null -> ErrorCommand(parsedOptions.error)
        parsedOptions.outputDir == null -> ErrorCommand("Missing required --out <output-dir> option.")
        else ->
            RunCommand(
                script = script,
                outputDir = parsedOptions.outputDir,
                variables = parsedOptions.variables,
                flags = parsedOptions.flags
            )
    }
}

private fun parseOutputDir(
    args: List<String>,
    startIndex: Int
): ParsedRunOptions {
    var outputDir: Path? = null
    val variables = linkedMapOf<String, String>()
    val flags = linkedSetOf<String>()
    var error: String? = null
    var index = startIndex
    while (index < args.size && error == null) {
        when (val token = args[index]) {
            OUTPUT_OPTION -> {
                val value = args.getOrNull(index + 1)
                error = validateOutputValue(value, outputDir != null)
                if (error == null) {
                    outputDir = Path.of(requireNotNull(value))
                    index += 2
                }
            }

            VARIABLE_OPTION -> {
                val value = args.getOrNull(index + 1)
                val parsedVariable = parseVariableValue(value)
                if (parsedVariable.error != null) {
                    error = parsedVariable.error
                } else if (variables.put(parsedVariable.key, parsedVariable.value) != null) {
                    error = "--var '${parsedVariable.key}' may only be specified once."
                } else {
                    index += 2
                }
            }

            FLAG_OPTION -> {
                val value = args.getOrNull(index + 1)
                val flag = parseFlagValue(value)
                if (flag == null) {
                    error = "Missing value for --flag option."
                } else if (!flags.add(flag)) {
                    error = "--flag '$flag' may only be specified once."
                } else {
                    index += 2
                }
            }

            else -> error = "Unknown option '$token'."
        }
    }

    return ParsedRunOptions(
        outputDir = outputDir,
        variables = variables.toMap(),
        flags = flags.toSet(),
        error = error
    )
}

private fun validateOutputValue(value: String?, outputDirAlreadySet: Boolean): String? =
    when {
        value == null || value.startsWith("--") -> "Missing value for --out option."
        outputDirAlreadySet -> "--out option may only be specified once."
        else -> null
    }

private fun parseVariableValue(value: String?): ParsedVariable =
    when {
        value == null || value.startsWith("--") -> ParsedVariable(error = "Missing value for --var option.")

        else -> {
            val separatorIndex = value.indexOf('=')
            val key =
                if (separatorIndex > 0) {
                    value.take(separatorIndex).trim()
                } else {
                    ""
                }
            if (separatorIndex <= 0 || key.isBlank()) {
                ParsedVariable(error = "Invalid --var value '$value'. Expected key=value.")
            } else {
                ParsedVariable(
                    key = key,
                    value = value.substring(separatorIndex + 1)
                )
            }
        }
    }

private fun parseFlagValue(value: String?): String? =
    value
        ?.takeUnless { it.startsWith("--") }
        ?.trim()
        ?.takeIf { it.isNotBlank() }
