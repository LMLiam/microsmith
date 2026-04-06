package io.github.lmliam.microsmith.cli

import io.github.lmliam.microsmith.cli.command.DoctorCommand
import io.github.lmliam.microsmith.cli.command.ErrorCommand
import io.github.lmliam.microsmith.cli.command.HelpCommand
import io.github.lmliam.microsmith.cli.command.IdeDoctorCommand
import io.github.lmliam.microsmith.cli.command.IdeRefreshCommand
import io.github.lmliam.microsmith.cli.command.InitCommand
import io.github.lmliam.microsmith.cli.command.RunCommand
import io.github.lmliam.microsmith.cli.command.VersionCommand
import io.github.lmliam.microsmith.cli.doctor.DoctorResult
import io.github.lmliam.microsmith.cli.doctor.runDoctorChecks
import io.github.lmliam.microsmith.cli.eventlog.EventLogWriter
import io.github.lmliam.microsmith.cli.eventlog.RunEventLogEntry
import io.github.lmliam.microsmith.cli.execution.CliDiagnosticEmitterFactory
import io.github.lmliam.microsmith.cli.execution.DoctorCommandHandler
import io.github.lmliam.microsmith.cli.execution.IdeDoctorCommandHandler
import io.github.lmliam.microsmith.cli.execution.IdeRefreshCommandHandler
import io.github.lmliam.microsmith.cli.execution.InitCommandHandler
import io.github.lmliam.microsmith.cli.execution.RunCommandHandler
import io.github.lmliam.microsmith.cli.execution.UsageErrorCommandHandler
import io.github.lmliam.microsmith.cli.ide.IdeDoctorResult
import io.github.lmliam.microsmith.cli.ide.IdeHelperRefreshResult
import io.github.lmliam.microsmith.cli.ide.refreshIdeHelperProject
import io.github.lmliam.microsmith.cli.ide.runIdeHelperDoctor
import io.github.lmliam.microsmith.cli.init.InitBootstrapResult
import io.github.lmliam.microsmith.cli.init.runInitBootstrap
import io.github.lmliam.microsmith.cli.parsing.parseCliArgs
import io.github.lmliam.microsmith.cli.plugins.PluginResolutionResult
import io.github.lmliam.microsmith.cli.plugins.resolvePlugins
import io.github.lmliam.microsmith.cli.provider.verifyBuiltinProviders
import io.github.lmliam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

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
    private val doctorRunner: () -> DoctorResult = {
        runDoctorChecks(providerValidator = providerValidator)
    },
    private val versionProvider: () -> String = ::resolveCliVersion,
    private val initRunner: (InitCommand) -> InitBootstrapResult = ::runInitBootstrap,
    private val ideRefreshRunner: (IdeRefreshCommand) -> IdeHelperRefreshResult = ::refreshIdeHelperProject,
    private val ideDoctorRunner: (IdeDoctorCommand) -> IdeDoctorResult = ::runIdeHelperDoctor,
    private val eventLogWriter: (Path, RunEventLogEntry) -> Unit = EventLogWriter::writeEventLog,
) {
    private val emitterFactory = CliDiagnosticEmitterFactory(stdout = stdout, stderr = stderr)
    private val usageErrorHandler = UsageErrorCommandHandler(emitterFactory = emitterFactory, stderr = stderr)
    private val runCommandHandler =
        RunCommandHandler(
            emitterFactory = emitterFactory,
            providerValidator = providerValidator,
            pluginResolver = pluginResolver,
            scriptRunner = scriptRunner,
            eventLogWriter = eventLogWriter,
        )
    private val doctorCommandHandler =
        DoctorCommandHandler(
            emitterFactory = emitterFactory,
            doctorRunner = doctorRunner,
        )
    private val initCommandHandler =
        InitCommandHandler(
            emitterFactory = emitterFactory,
            initRunner = initRunner,
        )
    private val ideRefreshCommandHandler =
        IdeRefreshCommandHandler(
            emitterFactory = emitterFactory,
            ideRefreshRunner = ideRefreshRunner,
        )
    private val ideDoctorCommandHandler =
        IdeDoctorCommandHandler(
            emitterFactory = emitterFactory,
            ideDoctorRunner = ideDoctorRunner,
        )

    fun run(args: Array<String>): Int = when (val parsed = parseCliArgs(args.toList())) {
        is HelpCommand -> {
            stdout(HELP_TEXT.trimIndent())
            0
        }

        is ErrorCommand -> usageErrorHandler.execute(parsed)

        is RunCommand -> runCommandHandler.execute(parsed)

        is DoctorCommand -> doctorCommandHandler.execute(parsed)

        is VersionCommand -> {
            stdout("microsmith ${versionProvider()}")
            0
        }

        is InitCommand -> initCommandHandler.execute(parsed)

        is IdeRefreshCommand -> ideRefreshCommandHandler.execute(parsed)

        is IdeDoctorCommand -> ideDoctorCommandHandler.execute(parsed)
    }
}
