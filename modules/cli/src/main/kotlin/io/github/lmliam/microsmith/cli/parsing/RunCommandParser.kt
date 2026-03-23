package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.command.CliCommand
import io.github.lmliam.microsmith.cli.command.ErrorCommand
import io.github.lmliam.microsmith.cli.command.RunCommand
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
    parsedOptions.error?.let { return ErrorCommand(it) }
    return RunCommand(
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
