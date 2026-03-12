package me.liam.microsmith.cli.execution

import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import me.liam.microsmith.cli.diagnostics.CliFailureCode
import me.liam.microsmith.cli.eventlog.RunEventLogEntry
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Path

internal class RunCompletionReporter(
    private val eventLogWriter: (Path, RunEventLogEntry) -> Unit,
) {
    fun complete(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        runResult: ScriptRunResult,
    ): Int = when (runResult) {
        is ScriptRunSuccess -> completeSuccessfulRun(command, emitter, context, runResult)
        is ScriptRunFailure -> completeFailedRun(command, emitter, context, runResult)
    }

    fun completeFailure(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        code: CliFailureCode,
    ): Int = completeRun(
        command = command,
        emitter = emitter,
        context = context,
        exitCode = code.exitCode,
        status = RunExecutionStatus.FAILURE,
        failureCode = code,
    )

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
            status = RunExecutionStatus.SUCCESS,
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
            status = RunExecutionStatus.FAILURE,
            failureCode = failureCode,
        )
    }

    private fun completeRun(
        command: RunCommand,
        emitter: CliDiagnosticEmitter,
        context: RunExecutionContext,
        exitCode: Int,
        status: RunExecutionStatus,
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

    private fun mapScriptFailureToCode(type: ScriptFailureType): CliFailureCode = when (type) {
        ScriptFailureType.VALIDATION -> CliFailureCode.SCRIPT_VALIDATION_FAILED
        ScriptFailureType.COMPILATION -> CliFailureCode.SCRIPT_COMPILATION_FAILED
        ScriptFailureType.EVALUATION -> CliFailureCode.SCRIPT_EVALUATION_FAILED
        ScriptFailureType.HOST -> CliFailureCode.SCRIPT_HOST_FAILED
    }
}
