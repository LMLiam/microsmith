package me.liam.microsmith.cli

import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.diagnostics.DiagnosticFormat
import me.liam.microsmith.cli.doctor.DoctorCheckStatus
import me.liam.microsmith.cli.doctor.DoctorResult
import me.liam.microsmith.cli.doctor.runDoctorChecks
import me.liam.microsmith.cli.eventlog.EventLogWriter
import me.liam.microsmith.cli.eventlog.RunEventLogEntry
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
    }

    private fun runCommand(command: RunCommand): Int {
        val emitter = createEmitter(command.diagnosticsFormat, command.verbose)
        val context = RunExecutionContext()
        val prepared = prepareRun(command, emitter, context)

        return when (prepared) {
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

        return when (val resolvedPlugins = pluginResolver(command)) {
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

private sealed interface PreparedRun {
    data class Ready(
        val result: ScriptRunResult,
    ) : PreparedRun

    data class Failure(
        val code: CliFailureCode,
    ) : PreparedRun
}

private data class RunExecutionContext(
    var resolverStatus: String = "skipped",
    var lockfilePath: Path? = null,
)
