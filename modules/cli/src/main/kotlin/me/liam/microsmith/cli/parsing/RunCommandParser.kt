package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Path

internal fun parseRunCommand(args: List<String>): CliCommand {
    val (script, scriptError) = parseScriptArg(args.getOrNull(1))
    return when {
        scriptError != null -> ErrorCommand(scriptError)
        script == null -> ErrorCommand("Missing <script.microsmith.kts> argument for run command.")
        else -> parseRunOptionsCommand(script, args, startIndex = 2)
    }
}

private fun parseScriptArg(scriptArg: String?): Pair<Path?, String?> = when {
    scriptArg == null || scriptArg.startsWith("--") ->
        null to "Missing <script.microsmith.kts> argument for run command."

    !scriptArg.endsWith(SCRIPT_EXTENSION) ->
        null to "Script file must use the .microsmith.kts extension."

    else -> Path.of(scriptArg) to null
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
                isolationMode = parsedOptions.isolationMode,
                diagnosticsFormat = parsedOptions.diagnosticsFormat,
                verbose = parsedOptions.verbose,
                eventLog = parsedOptions.eventLog,
            )
    }
}
