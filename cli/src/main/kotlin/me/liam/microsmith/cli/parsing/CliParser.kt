package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal const val RUN_COMMAND = "run"
internal const val DOCTOR_COMMAND = "doctor"
internal const val OUTPUT_OPTION = "--out"
internal const val VARIABLE_OPTION = "--var"
internal const val FLAG_OPTION = "--flag"
internal const val PLUGIN_OPTION = "--plugin"
internal const val PLUGIN_JAR_OPTION = "--plugin-jar"
internal const val OFFLINE_OPTION = "--offline"
internal const val REPOSITORY_OPTION = "--repository"
internal const val ISOLATION_OPTION = "--isolation"
internal const val DIAGNOSTICS_OPTION = "--diagnostics"
internal const val VERBOSE_OPTION = "--verbose"
internal const val AUDIT_LOG_OPTION = "--audit-log"
private const val SCRIPT_EXTENSION = ".microsmith.kts"
private val HELP_COMMANDS = setOf("--help", "-h", "help")

internal fun parseCliArgs(args: List<String>): CliCommand {
    val command = args.firstOrNull()
    return when {
        command == null || command in HELP_COMMANDS -> HelpCommand
        command == RUN_COMMAND -> parseRunCommand(args)
        command == DOCTOR_COMMAND -> parseDoctorCommand(args)
        else -> ErrorCommand("Unknown command '$command'.")
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
                isolationMode = parsedOptions.isolationMode,
                diagnosticsFormat = parsedOptions.diagnosticsFormat,
                verbose = parsedOptions.verbose,
                auditLog = parsedOptions.auditLog,
            )
    }
}

private fun parseDoctorCommand(args: List<String>): CliCommand {
    val parsed = parseDoctorOptions(args = args, startIndex = 1)
    return if (parsed.error != null) {
        ErrorCommand(parsed.error)
    } else {
        DoctorCommand(
            diagnosticsFormat = parsed.diagnosticsFormat,
            verbose = parsed.verbose,
        )
    }
}

private fun parseDoctorOptions(args: List<String>, startIndex: Int): ParsedDoctorOptions {
    var diagnosticsFormat = DiagnosticFormat.TEXT
    var diagnosticsSpecified = false
    var verbose = false
    var error: String? = null
    var index = startIndex

    while (index < args.size && error == null) {
        when (val token = args[index]) {
            DIAGNOSTICS_OPTION -> {
                val value = args.getOrNull(index + 1)
                val parsedFormat = parseDiagnosticFormat(value)
                error =
                    when {
                        value == null || value.startsWith("--") -> "Missing value for --diagnostics option."
                        diagnosticsSpecified -> "--diagnostics may only be specified once."
                        parsedFormat == null ->
                            "Invalid --diagnostics value '$value'. Expected 'text' or 'json'."
                        else -> null
                    }
                if (error == null) {
                    diagnosticsFormat = requireNotNull(parsedFormat)
                    diagnosticsSpecified = true
                    index += 2
                }
            }

            VERBOSE_OPTION -> {
                if (verbose) {
                    error = "--verbose may only be specified once."
                } else {
                    verbose = true
                    index += 1
                }
            }

            else -> error = "Unknown option '$token'."
        }
    }

    return ParsedDoctorOptions(
        diagnosticsFormat = diagnosticsFormat,
        verbose = verbose,
        error = error,
    )
}

private data class ParsedDoctorOptions(
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val error: String?,
)
