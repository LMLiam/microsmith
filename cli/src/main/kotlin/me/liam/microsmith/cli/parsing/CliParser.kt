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
    val (outputDir, variables, flags, outputError) = parseOutputDir(args, startIndex)
    return when {
        outputError != null -> ErrorCommand(outputError)
        outputDir == null -> ErrorCommand("Missing required --out <output-dir> option.")
        else ->
            RunCommand(
                script = script,
                outputDir = outputDir,
                variables = variables,
                flags = flags
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
    var index = startIndex
    while (index < args.size) {
        val token = args[index]
        when (token) {
            OUTPUT_OPTION -> {
                val value = args.getOrNull(index + 1)
                val error = validateOutputValue(value, outputDir != null)
                if (error != null) {
                    return ParsedRunOptions(outputDir, variables.toMap(), flags.toSet(), error)
                }

                outputDir = Path.of(requireNotNull(value))
                index += 2
            }
            VARIABLE_OPTION -> {
                val value = args.getOrNull(index + 1)
                val (key, parsedValue, error) = parseVariableValue(value)
                if (error != null) {
                    return ParsedRunOptions(outputDir, variables.toMap(), flags.toSet(), error)
                }
                if (variables.put(key, parsedValue) != null) {
                    return ParsedRunOptions(
                        outputDir,
                        variables.toMap(),
                        flags.toSet(),
                        "--var '$key' may only be specified once."
                    )
                }
                index += 2
            }
            FLAG_OPTION -> {
                val value = args.getOrNull(index + 1)
                val flag = parseFlagValue(value)
                    ?: return ParsedRunOptions(
                        outputDir,
                        variables.toMap(),
                        flags.toSet(),
                        "Missing value for --flag option."
                    )
                if (!flags.add(flag)) {
                    return ParsedRunOptions(
                        outputDir,
                        variables.toMap(),
                        flags.toSet(),
                        "--flag '$flag' may only be specified once."
                    )
                }
                index += 2
            }
            else -> {
                return ParsedRunOptions(outputDir, variables.toMap(), flags.toSet(), "Unknown option '$token'.")
            }
        }
    }

    return ParsedRunOptions(outputDir, variables.toMap(), flags.toSet(), null)
}

private fun validateOutputValue(value: String?, outputDirAlreadySet: Boolean): String? =
    when {
        value == null || value.startsWith("--") -> "Missing value for --out option."
        outputDirAlreadySet -> "--out option may only be specified once."
        else -> null
    }

private fun parseVariableValue(value: String?): ParsedVariable {
    if (value == null || value.startsWith("--")) {
        return ParsedVariable(error = "Missing value for --var option.")
    }

    val separatorIndex = value.indexOf('=')
    if (separatorIndex <= 0) {
        return ParsedVariable(error = "Invalid --var value '$value'. Expected key=value.")
    }

    val key = value.substring(0, separatorIndex).trim()
    if (key.isBlank()) {
        return ParsedVariable(error = "Invalid --var value '$value'. Expected key=value.")
    }

    val parsedValue = value.substring(separatorIndex + 1)
    return ParsedVariable(key = key, value = parsedValue)
}

private fun parseFlagValue(value: String?): String? =
    value
        ?.takeUnless { it.startsWith("--") }
        ?.trim()
        ?.takeIf { it.isNotBlank() }
