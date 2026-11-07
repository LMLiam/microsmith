package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.command.RunCommand
import io.github.lmliam.microsmith.cli.eventlog.EventLogWriter
import io.github.lmliam.microsmith.cli.eventlog.RunEventLogEntry
import io.github.lmliam.microsmith.cli.plugins.PluginResolutionResult
import io.github.lmliam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

internal class RunCommandHandler(
    private val emitterFactory: CliDiagnosticEmitterFactory,
    providerValidator: () -> List<String>,
    pluginResolver: (RunCommand) -> PluginResolutionResult,
    scriptRunner: (RunCommand, List<Path>) -> ScriptRunResult = { command, pluginClasspath ->
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
    eventLogWriter: (Path, RunEventLogEntry) -> Unit = EventLogWriter::writeEventLog,
) {
    private val preparationService = RunPreparationService(providerValidator, pluginResolver, scriptRunner)
    private val completionReporter = RunCompletionReporter(eventLogWriter)

    fun execute(command: RunCommand): Int {
        val emitter = emitterFactory.create(command.diagnosticsFormat, command.verbose)
        val context = RunExecutionContext()

        return when (val prepared = preparationService.prepare(command, emitter, context)) {
            is PreparedRun.Failure -> completionReporter.completeFailure(command, emitter, context, prepared.code)
            is PreparedRun.Ready -> completionReporter.complete(command, emitter, context, prepared.result)
        }
    }
}
