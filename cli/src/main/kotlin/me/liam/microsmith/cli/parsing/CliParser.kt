package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Path

internal const val RUN_COMMAND = "run"
internal const val OUTPUT_OPTION = "--out"
internal const val VARIABLE_OPTION = "--var"
internal const val FLAG_OPTION = "--flag"
internal const val PLUGIN_OPTION = "--plugin"
internal const val PLUGIN_JAR_OPTION = "--plugin-jar"
internal const val OFFLINE_OPTION = "--offline"
internal const val REPOSITORY_OPTION = "--repository"
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
        else -> parseRunOptionsCommand(script, args, startIndex = 2)
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

private fun parseRunOptionsCommand(script: Path, args: List<String>, startIndex: Int): CliCommand {
    val parsedOptions = parseRunOptions(args, startIndex)
    return when {
        parsedOptions.error != null -> ErrorCommand(parsedOptions.error)
        parsedOptions.outputDir == null -> ErrorCommand("Missing required --out <output-dir> option.")
        else ->
            RunCommand(
                script = script,
                outputDir = parsedOptions.outputDir,
                variables = parsedOptions.variables,
                flags = parsedOptions.flags,
                plugins = parsedOptions.plugins,
                pluginJars = parsedOptions.pluginJars,
                offline = parsedOptions.offline,
                repositoryOverride = parsedOptions.repositoryOverride,
            )
    }
}
