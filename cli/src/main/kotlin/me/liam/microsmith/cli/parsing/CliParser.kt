package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.command.IdeRefreshCommand
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.command.VersionCommand
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import java.nio.file.Path

internal const val INIT_COMMAND = "init"
internal const val RUN_COMMAND = "run"
internal const val DOCTOR_COMMAND = "doctor"
internal const val IDE_COMMAND = "ide"
internal const val IDE_REFRESH_SUBCOMMAND = "refresh"
internal const val IDE_DOCTOR_SUBCOMMAND = "doctor"
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
internal const val EVENT_LOG_OPTION = "--event-log"
internal const val REPO_ROOT_OPTION = "--repo-root"
internal const val FORCE_OPTION = "--force"
internal const val SKIP_IDE_HELPER_OPTION = "--skip-ide-helper"
private const val SCRIPT_EXTENSION = ".microsmith.kts"
private val HELP_COMMANDS = setOf("--help", "-h", "help")
private val VERSION_COMMANDS = setOf("--version")

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
                eventLog = parsedOptions.eventLog,
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

private fun parseIdeCommand(args: List<String>): CliCommand {
    val subcommand = args.getOrNull(1)
    return when {
        subcommand == null || subcommand in HELP_COMMANDS ->
            ErrorCommand("Missing <refresh|doctor> subcommand for ide command.")

        subcommand == IDE_REFRESH_SUBCOMMAND -> parseIdeRefreshCommand(args = args, startIndex = 2)

        subcommand == IDE_DOCTOR_SUBCOMMAND -> parseIdeDoctorCommand(args = args, startIndex = 2)

        else ->
            ErrorCommand("Unknown ide subcommand '$subcommand'. Expected 'refresh' or 'doctor'.")
    }
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
    val state = IdeOptionsState()
    var index = startIndex

    while (index < args.size && state.error == null) {
        val consumed =
            when (val token = args[index]) {
                DIAGNOSTICS_OPTION -> state.consumeDiagnostics(args = args, index = index)
                VERBOSE_OPTION -> state.consumeVerbose()
                REPO_ROOT_OPTION -> state.consumeRepoRoot(args = args, index = index)
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

    return state.toParsedIdeOptions()
}

private fun parseInitCommand(args: List<String>): CliCommand {
    val parsed = parseInitOptions(args = args, startIndex = 1)
    return if (parsed.error != null) {
        ErrorCommand(parsed.error)
    } else {
        InitCommand(
            projectRoot = parsed.projectRoot,
            diagnosticsFormat = parsed.diagnosticsFormat,
            verbose = parsed.verbose,
            force = parsed.force,
            skipIdeHelper = parsed.skipIdeHelper,
        )
    }
}

private fun parseInitOptions(args: List<String>, startIndex: Int): ParsedInitOptions {
    val state = InitOptionsState()
    var index = startIndex

    while (index < args.size && state.error == null) {
        val consumed =
            when (val token = args[index]) {
                DIAGNOSTICS_OPTION -> state.consumeDiagnostics(args = args, index = index)
                VERBOSE_OPTION -> state.consumeVerbose()
                REPO_ROOT_OPTION -> state.consumeRepoRoot(args = args, index = index)
                FORCE_OPTION -> state.consumeForce()
                SKIP_IDE_HELPER_OPTION -> state.consumeSkipIdeHelper()
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

    return state.toParsedInitOptions()
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

private class IdeOptionsState {
    private var diagnosticsFormat = DiagnosticFormat.TEXT
    private var diagnosticsSpecified = false
    private var verbose = false
    private var projectRoot = Path.of(".")
    private var projectRootSpecified = false

    var error: String? = null
        private set

    fun consumeDiagnostics(args: List<String>, index: Int): Int {
        val value = args.getOrNull(index + 1)
        val parsedFormat = parseDiagnosticFormat(value)
        error =
            when {
                value == null || value.startsWith("--") -> "Missing value for --diagnostics option."
                diagnosticsSpecified -> "--diagnostics may only be specified once."
                parsedFormat == null -> "Invalid --diagnostics value '$value'. Expected 'text' or 'json'."
                else -> null
            }

        if (error != null) {
            return 0
        }

        diagnosticsFormat = requireNotNull(parsedFormat)
        diagnosticsSpecified = true
        return 2
    }

    fun consumeVerbose(): Int {
        if (verbose) {
            error = "--verbose may only be specified once."
            return 0
        }

        verbose = true
        return 1
    }

    fun consumeRepoRoot(args: List<String>, index: Int): Int {
        val value = args.getOrNull(index + 1)
        error =
            when {
                value == null || value.startsWith("--") -> "Missing value for --repo-root option."
                projectRootSpecified -> "--repo-root may only be specified once."
                else -> null
            }

        if (error != null) {
            return 0
        }

        projectRoot = Path.of(value)
        projectRootSpecified = true
        return 2
    }

    fun consumeUnknownOption(token: String) {
        error = "Unknown option '$token'."
    }

    fun toParsedIdeOptions(): ParsedIdeOptions {
        return ParsedIdeOptions(
            projectRoot = projectRoot,
            diagnosticsFormat = diagnosticsFormat,
            verbose = verbose,
            error = error,
        )
    }
}

private class InitOptionsState {
    private var diagnosticsFormat = DiagnosticFormat.TEXT
    private var diagnosticsSpecified = false
    private var verbose = false
    private var force = false
    private var skipIdeHelper = false
    private var projectRoot = Path.of(".")
    private var projectRootSpecified = false

    var error: String? = null
        private set

    fun consumeDiagnostics(args: List<String>, index: Int): Int {
        val value = args.getOrNull(index + 1)
        val parsedFormat = parseDiagnosticFormat(value)
        error =
            when {
                value == null || value.startsWith("--") -> "Missing value for --diagnostics option."
                diagnosticsSpecified -> "--diagnostics may only be specified once."
                parsedFormat == null -> "Invalid --diagnostics value '$value'. Expected 'text' or 'json'."
                else -> null
            }

        if (error != null) {
            return 0
        }

        diagnosticsFormat = requireNotNull(parsedFormat)
        diagnosticsSpecified = true
        return 2
    }

    fun consumeVerbose(): Int {
        if (verbose) {
            error = "--verbose may only be specified once."
            return 0
        }

        verbose = true
        return 1
    }

    fun consumeRepoRoot(args: List<String>, index: Int): Int {
        val value = args.getOrNull(index + 1)
        error =
            when {
                value == null || value.startsWith("--") -> "Missing value for --repo-root option."
                projectRootSpecified -> "--repo-root may only be specified once."
                else -> null
            }

        if (error != null) {
            return 0
        }

        projectRoot = Path.of(value)
        projectRootSpecified = true
        return 2
    }

    fun consumeForce(): Int {
        if (force) {
            error = "--force may only be specified once."
            return 0
        }

        force = true
        return 1
    }

    fun consumeSkipIdeHelper(): Int {
        if (skipIdeHelper) {
            error = "--skip-ide-helper may only be specified once."
            return 0
        }

        skipIdeHelper = true
        return 1
    }

    fun consumeUnknownOption(token: String) {
        error = "Unknown option '$token'."
    }

    fun toParsedInitOptions(): ParsedInitOptions {
        return ParsedInitOptions(
            projectRoot = projectRoot,
            diagnosticsFormat = diagnosticsFormat,
            verbose = verbose,
            force = force,
            skipIdeHelper = skipIdeHelper,
            error = error,
        )
    }
}

private data class ParsedIdeOptions(
    val projectRoot: Path,
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val error: String?,
)

private data class ParsedInitOptions(
    val projectRoot: Path,
    val diagnosticsFormat: DiagnosticFormat,
    val verbose: Boolean,
    val force: Boolean,
    val skipIdeHelper: Boolean,
    val error: String?,
)
