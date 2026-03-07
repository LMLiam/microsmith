package me.liam.microsmith.cli

import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.command.IdeRefreshCommand
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.command.VersionCommand
import me.liam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import me.liam.microsmith.cli.doctor.DoctorCheckStatus
import me.liam.microsmith.cli.doctor.DoctorResult
import me.liam.microsmith.cli.doctor.runDoctorChecks
import me.liam.microsmith.cli.eventlog.EventLogWriter
import me.liam.microsmith.cli.eventlog.RunEventLogEntry
import me.liam.microsmith.cli.ide.IdeDoctorResult
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import me.liam.microsmith.cli.ide.refreshIdeHelperProject
import me.liam.microsmith.cli.ide.runIdeHelperDoctor
import me.liam.microsmith.cli.init.InitBootstrapResult
import me.liam.microsmith.cli.init.InitConflictException
import me.liam.microsmith.cli.init.InitValidationException
import me.liam.microsmith.cli.init.describeForSummary
import me.liam.microsmith.cli.init.runInitBootstrap
import me.liam.microsmith.cli.parsing.parseCliArgs
import me.liam.microsmith.cli.plugins.PluginResolutionResult
import me.liam.microsmith.cli.plugins.resolvePlugins
import me.liam.microsmith.cli.provider.verifyBuiltinProviders
import me.liam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Path
import java.util.ServiceConfigurationError

private const val RUN_STATUS_SUCCESS = "success"
private const val RUN_STATUS_FAILURE = "failure"

internal class MicrosmithCli(
    private val stdout: (String) -> Unit = ::println,
    private val stderr: (String) -> Unit = { System.err.println(it) },
    private val providerValidator: () -> List<String> = ::verifyBuiltinProviders,
    private val pluginResolver: (RunCommand) -> PluginResolutionResult = ::resolvePlugins,
    private val scriptRunner: (RunCommand, List<Path>) -> ScriptRunResult = { command, pluginClasspath ->
        MicrosmithScriptHost().run(
            ScriptRunRequest(
                script = command.script,
                outputDir = command.outputDir,
                variables = command.variables,
                flags = command.flags,
                pluginClasspath = pluginClasspath,
                isolationMode = command.isolationMode,
            ),
        )
    },
    private val doctorRunner: ((() -> List<String>) -> DoctorResult) = { validator ->
        runDoctorChecks(providerValidator = validator)
    },
    private val versionProvider: () -> String = ::resolveCliVersion,
    private val initRunner: (InitCommand) -> InitBootstrapResult = ::runInitBootstrap,
    private val ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult = ::refreshIdeHelperProject,
    private val ideDoctorRunner: (IdeDoctorCommand) -> IdeDoctorResult = ::runIdeHelperDoctor,
    private val eventLogWriter: (Path, RunEventLogEntry) -> Unit = EventLogWriter::writeEventLog,
) {
    fun run(args: Array<String>): Int = when (val parsed = parseCliArgs(args.toList())) {
        is HelpCommand -> {
            stdout(HELP_TEXT.trimIndent())
            0
        }

        is ErrorCommand -> {
            val emitter =
                CliDiagnosticEmitter(
                    format = DiagnosticFormat.TEXT,
                    verbose = false,
                    stdout = stdout,
                    stderr = stderr,
                )
            emitter.error(CliFailureCode.USAGE_ERROR, parsed.message)
            stderr("")
            stderr(HELP_TEXT.trimIndent())
            CliFailureCode.USAGE_ERROR.exitCode
        }

        is RunCommand -> runCommand(parsed)

        is DoctorCommand -> runDoctor(parsed)

        is VersionCommand -> {
            stdout("microsmith ${versionProvider()}")
            0
        }

        is InitCommand -> runInit(parsed)

        is IdeRefreshCommand -> runIdeRefresh(parsed)

        is IdeDoctorCommand -> runIdeDoctor(parsed)
    }

    private fun runCommand(command: RunCommand): Int {
        val emitter = createEmitter(command.diagnosticsFormat, command.verbose)
        val context = RunExecutionContext()

        return when (val prepared = prepareRun(command, emitter, context)) {
            is PreparedRun.Failure ->
                completeRun(
                    command = command,
                    emitter = emitter,
                    context = context,
                    exitCode = prepared.code.exitCode,
                    status = RUN_STATUS_FAILURE,
                    failureCode = prepared.code,
                )

            is PreparedRun.Ready ->
                completeFromRunResult(
                    command = command,
                    emitter = emitter,
                    context = context,
                    runResult = prepared.result,
                )
        }
    }

    private fun runDoctor(command: DoctorCommand): Int {
        val emitter = createEmitter(command.diagnosticsFormat, command.verbose)
        val result = doctorRunner(providerValidator)
        result.checks.forEach { check ->
            val details = mapOf("check" to check.id) + check.details
            if (check.status == DoctorCheckStatus.PASS) {
                emitter.info("doctor/${check.id}: ${check.message}", details)
            } else {
                emitter.error(CliFailureCode.DOCTOR_FAILED, "doctor/${check.id}: ${check.message}", details)
            }
        }

        return if (result.hasFailures) {
            emitter.error(CliFailureCode.DOCTOR_FAILED, "Doctor detected environment issues.")
            CliFailureCode.DOCTOR_FAILED.exitCode
        } else {
            emitter.info("Doctor checks passed.")
            0
        }
    }

    private fun runIdeRefresh(command: IdeRefreshCommand): Int {
        val emitter = createEmitter(command.diagnosticsFormat, command.verbose)
        val result =
            runCatching {
                ideRefreshRunner(command)
            }.getOrElse { error ->
                emitter.error(
                    CliFailureCode.IDE_HELPER_FAILED,
                    error.message ?: "JetBrains IDE helper generation failed.",
                )
                return CliFailureCode.IDE_HELPER_FAILED.exitCode
            }

        val helperRoot = result.helperRoot.toAbsolutePath().normalize()
        val refreshed = result.updatedFiles.size
        val state = if (refreshed == 0) "unchanged" else "updated"
        emitter.info(
            "JetBrains IDE helper is $state at '$helperRoot'.",
            details =
            mapOf(
                "projectRoot" to result.projectRoot.toAbsolutePath().normalize().toString(),
                "helperRoot" to helperRoot.toString(),
                "updatedFiles" to refreshed.toString(),
                "classpathEntries" to result.classpathEntries.size.toString(),
            ),
        )
        emitter.info(
            "Import '${helperRoot.resolve("build.gradle.kts")}' as a Gradle project in JetBrains IDEs.",
        )
        return 0
    }

    private fun runIdeDoctor(command: IdeDoctorCommand): Int {
        val emitter = createEmitter(command.diagnosticsFormat, command.verbose)
        val result =
            runCatching {
                ideDoctorRunner(command)
            }.getOrElse { error ->
                emitter.error(
                    CliFailureCode.IDE_DOCTOR_FAILED,
                    error.message ?: "JetBrains IDE helper doctor failed unexpectedly.",
                )
                return CliFailureCode.IDE_DOCTOR_FAILED.exitCode
            }

        result.checks.forEach { check ->
            if (check.passed) {
                emitter.info("ide-doctor/${check.id}: ${check.message}", check.details)
            } else {
                emitter.error(
                    CliFailureCode.IDE_DOCTOR_FAILED,
                    "ide-doctor/${check.id}: ${check.message}",
                    check.details,
                )
            }
        }

        return if (result.hasFailures) {
            emitter.error(CliFailureCode.IDE_DOCTOR_FAILED, "JetBrains IDE helper doctor detected issues.")
            CliFailureCode.IDE_DOCTOR_FAILED.exitCode
        } else {
            emitter.info("JetBrains IDE helper doctor checks passed.")
            0
        }
    }

    private fun runInit(command: InitCommand): Int {
        val emitter = createEmitter(command.diagnosticsFormat, command.verbose)
        val result =
            runCatching {
                initRunner(command)
            }.getOrElse { error ->
                val code =
                    when (error) {
                        is InitConflictException -> CliFailureCode.INIT_CONFLICT
                        is InitValidationException -> CliFailureCode.INIT_VALIDATION_FAILED
                        else -> CliFailureCode.INIT_RUNTIME_FAILED
                    }
                emitter.error(code, error.message ?: "Microsmith init failed.")
                return code.exitCode
            }

        val projectRoot = result.projectRoot.toAbsolutePath().normalize()
        emitter.info(
            "Microsmith init completed at '$projectRoot'.",
            details =
            mapOf(
                "projectRoot" to projectRoot.toString(),
                "repositoryType" to result.repositoryDetection.type.displayName,
                "matchedMarkers" to result.repositoryDetection.matchedMarkers.joinToString(separator = ","),
                "createdFiles" to result.createdFiles.size.toString(),
                "overwrittenFiles" to result.overwrittenFiles.size.toString(),
                "preservedFiles" to result.preservedFiles.size.toString(),
                "ideHelperUpdatedFiles" to (result.ideHelperResult?.updatedFiles?.size ?: 0).toString(),
                "ideHelperSkipped" to command.skipIdeHelper.toString(),
                "force" to command.force.toString(),
            ),
        )
        emitter.info("Detected repository type: ${result.repositoryDetection.describeForSummary()}.")
        emitInitBootstrapSummary(emitter = emitter, command = command, result = result)
        emitIdeHelperSummary(emitter = emitter, result = result)
        emitter.info("Next: microsmith run build.microsmith.kts --out ./generated")
        result.repositoryDetection.type.repoNativeOutputDirectory?.let { outputDirectory ->
            emitter.info(
                "Optional repository-native output path: " +
                    "microsmith run build.microsmith.kts --out $outputDirectory",
            )
        }
        return 0
    }

    private fun emitInitBootstrapSummary(
        emitter: CliDiagnosticEmitter,
        command: InitCommand,
        result: InitBootstrapResult,
    ) {
        if (result.createdFiles.isNotEmpty()) {
            emitter.info(
                "Created bootstrap files: ${result.createdFiles.formatForDisplay(result.projectRoot)}",
            )
        }
        if (result.overwrittenFiles.isNotEmpty()) {
            emitter.info(
                "Overwrote bootstrap files: ${result.overwrittenFiles.formatForDisplay(result.projectRoot)}",
            )
        }
        if (result.preservedFiles.isNotEmpty()) {
            val message =
                if (command.force) {
                    "Preserved bootstrap files already matching the managed templates"
                } else {
                    "Preserved existing bootstrap files"
                }
            emitter.info("$message: ${result.preservedFiles.formatForDisplay(result.projectRoot)}")
            if (!command.force) {
                emitter.info(
                    "Re-run with --force to replace existing regular bootstrap files " +
                        "with the managed templates.",
                )
            }
        }
        if (
            result.createdFiles.isEmpty() &&
            result.overwrittenFiles.isEmpty() &&
            result.preservedFiles.isEmpty()
        ) {
            emitter.info("Bootstrap completed with no managed file changes.")
        }
    }

    private fun emitIdeHelperSummary(emitter: CliDiagnosticEmitter, result: InitBootstrapResult) {
        val ideHelperResult = result.ideHelperResult
        if (ideHelperResult == null) {
            emitter.info(
                "JetBrains IDE helper generation was skipped. " +
                    "Run 'microsmith ide refresh' when you want IDE indexing.",
            )
            return
        }

        val helperRoot = ideHelperResult.helperRoot.toAbsolutePath().normalize()
        val state = if (ideHelperResult.updatedFiles.isEmpty()) "already current" else "updated"
        emitter.info("JetBrains IDE helper is $state at '$helperRoot'.")
        emitter.info("Import '${helperRoot.resolve("build.gradle.kts")}' as a Gradle project in JetBrains IDEs.")
    }

    private fun prepareRun(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
    ): PreparedRun {
        val providerErrors = collectProviderErrors()
        if (providerErrors.isNotEmpty()) {
            providerErrors.forEach { providerError ->
                emitter.error(CliFailureCode.PROVIDER_VALIDATION_FAILED, providerError)
            }
            return PreparedRun.Failure(CliFailureCode.PROVIDER_VALIDATION_FAILED)
        }

        val resolvedPlugins =
            runCatching {
                pluginResolver(command)
            }.getOrElse { error ->
                context.resolverStatus = RUN_STATUS_FAILURE
                emitter.error(
                    CliFailureCode.PLUGIN_RESOLUTION_FAILED,
                    "[unexpected] Plugin resolution failed unexpectedly.",
                    details = mapOf("exceptionType" to (error::class.simpleName ?: "unknown")),
                )
                return PreparedRun.Failure(CliFailureCode.PLUGIN_RESOLUTION_FAILED)
            }

        return when (resolvedPlugins) {
            is PluginResolutionResult.Failure -> {
                context.resolverStatus = RUN_STATUS_FAILURE
                resolvedPlugins.diagnostics.forEach { diagnostic ->
                    emitter.error(CliFailureCode.PLUGIN_RESOLUTION_FAILED, diagnostic)
                }
                PreparedRun.Failure(CliFailureCode.PLUGIN_RESOLUTION_FAILED)
            }

            is PluginResolutionResult.Success -> {
                context.resolverStatus = RUN_STATUS_SUCCESS
                context.lockfilePath = resolvedPlugins.lockfilePath
                PreparedRun.Ready(scriptRunner(command, resolvedPlugins.classpath))
            }
        }
    }

    private fun completeFromRunResult(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        runResult: ScriptRunResult,
    ): Int = when (runResult) {
        is ScriptRunSuccess ->
            completeSuccessfulRun(
                command = command,
                emitter = emitter,
                context = context,
                runResult = runResult,
            )

        is ScriptRunFailure ->
            completeFailedRun(
                command = command,
                emitter = emitter,
                context = context,
                runResult = runResult,
            )
    }

    private fun completeSuccessfulRun(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        runResult: ScriptRunSuccess,
    ): Int {
        runResult.warnings.forEach { warning ->
            emitter.warn(warning)
        }
        val cacheState = if (runResult.cacheHit) "hit" else "miss"
        emitter.info(
            "Generated script '${command.script}' into '${command.outputDir}' " +
                "(compile-cache=$cacheState, elapsed=${runResult.elapsedMillis}ms).",
            details =
            mapOf(
                "script" to command.script.toAbsolutePath().normalize().toString(),
                "outputDir" to command.outputDir.toAbsolutePath().normalize().toString(),
                "compileCache" to cacheState,
                "elapsedMillis" to runResult.elapsedMillis.toString(),
                "offline" to command.offline.toString(),
                "isolationMode" to command.isolationMode.cliValue,
                "pluginCount" to command.plugins.size.toString(),
                "pluginJarCount" to command.pluginJars.size.toString(),
                "lockfile" to (context.lockfilePath?.toAbsolutePath()?.normalize()?.toString() ?: "<none>"),
            ),
        )
        return completeRun(
            command = command,
            emitter = emitter,
            context = context,
            exitCode = 0,
            status = RUN_STATUS_SUCCESS,
            cacheHit = runResult.cacheHit,
            elapsedMillis = runResult.elapsedMillis,
        )
    }

    private fun completeFailedRun(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        runResult: ScriptRunFailure,
    ): Int {
        val failureCode = mapScriptFailureToCode(runResult.type)
        if (runResult.diagnostics.isEmpty()) {
            emitter.error(failureCode, "Script execution failed.")
        } else {
            runResult.diagnostics.forEach { diagnostic ->
                emitter.error(failureCode, diagnostic)
            }
        }
        return completeRun(
            command = command,
            emitter = emitter,
            context = context,
            exitCode = failureCode.exitCode,
            status = RUN_STATUS_FAILURE,
            failureCode = failureCode,
        )
    }

    private fun completeRun(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        exitCode: Int,
        status: String,
        failureCode: CliFailureCode? = null,
        cacheHit: Boolean? = null,
        elapsedMillis: Long? = null,
    ): Int {
        command.eventLog?.let { eventLogPath ->
            runCatching {
                eventLogWriter(
                    eventLogPath,
                    RunEventLogEntry(
                        scriptPath = command.script,
                        outputPath = command.outputDir,
                        pluginCoordinates = command.plugins,
                        pluginJars = command.pluginJars,
                        offline = command.offline,
                        isolationMode = command.isolationMode.cliValue,
                        status = status,
                        exitCode = exitCode,
                        failureCode = failureCode,
                        resolverStatus = context.resolverStatus,
                        lockfilePath = context.lockfilePath,
                        cacheHit = cacheHit,
                        elapsedMillis = elapsedMillis,
                    ),
                )
            }.onFailure { error ->
                emitter.warn(
                    "Event log write failed: ${error.message ?: error::class.simpleName ?: "unknown error"}",
                    details = mapOf("eventLog" to eventLogPath.toAbsolutePath().normalize().toString()),
                )
            }
        }
        return exitCode
    }

    private fun createEmitter(format: DiagnosticFormat, verbose: Boolean): CliDiagnosticEmitter = CliDiagnosticEmitter(
        format = format,
        verbose = verbose,
        stdout = stdout,
        stderr = stderr,
    )

    private fun collectProviderErrors(): List<String> = try {
        providerValidator()
    } catch (error: ServiceConfigurationError) {
        val message = error.message ?: error::class.simpleName ?: "ServiceConfigurationError"
        listOf("Failed to load runtime service providers: $message")
    }

    private fun mapScriptFailureToCode(type: ScriptFailureType): CliFailureCode = when (type) {
        ScriptFailureType.VALIDATION -> CliFailureCode.SCRIPT_VALIDATION_FAILED
        ScriptFailureType.COMPILATION -> CliFailureCode.SCRIPT_COMPILATION_FAILED
        ScriptFailureType.EVALUATION -> CliFailureCode.SCRIPT_EVALUATION_FAILED
        ScriptFailureType.HOST -> CliFailureCode.SCRIPT_HOST_FAILED
    }
}

private fun List<Path>.formatForDisplay(projectRoot: Path): String = joinToString(separator = ", ") { path ->
    projectRoot.relativize(path.toAbsolutePath().normalize()).toString()
}

private sealed interface PreparedRun {
    data class Ready(val result: ScriptRunResult) : PreparedRun

    data class Failure(val code: CliFailureCode) : PreparedRun
}

private data class RunExecutionContext(var resolverStatus: String = "skipped", var lockfilePath: Path? = null)
