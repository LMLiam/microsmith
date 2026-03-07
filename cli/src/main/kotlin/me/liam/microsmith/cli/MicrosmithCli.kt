package me.liam.microsmith.cli

import me.liam.microsmith.cli.command.DoctorCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.IdeDoctorCommand
import me.liam.microsmith.cli.command.IdeRefreshCommand
import me.liam.microsmith.cli.command.InitCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.command.VersionCommand
import me.liam.microsmith.cli.doctor.DoctorResult
import me.liam.microsmith.cli.doctor.runDoctorChecks
import me.liam.microsmith.cli.eventlog.EventLogWriter
import me.liam.microsmith.cli.eventlog.RunEventLogEntry
import me.liam.microsmith.cli.execution.CliDiagnosticEmitterFactory
import me.liam.microsmith.cli.execution.DoctorCommandHandler
import me.liam.microsmith.cli.execution.IdeDoctorCommandHandler
import me.liam.microsmith.cli.execution.IdeRefreshCommandHandler
import me.liam.microsmith.cli.execution.InitCommandHandler
import me.liam.microsmith.cli.execution.RunCommandHandler
import me.liam.microsmith.cli.execution.UsageErrorCommandHandler
import me.liam.microsmith.cli.ide.IdeDoctorResult
import me.liam.microsmith.cli.ide.IdeHelperRefreshResult
import me.liam.microsmith.cli.ide.refreshIdeHelperProject
import me.liam.microsmith.cli.ide.runIdeHelperDoctor
import me.liam.microsmith.cli.init.InitBootstrapResult
import me.liam.microsmith.cli.init.runInitBootstrap
import me.liam.microsmith.cli.parsing.parseCliArgs
import me.liam.microsmith.cli.plugins.PluginResolutionResult
import me.liam.microsmith.cli.plugins.resolvePlugins
import me.liam.microsmith.cli.provider.verifyBuiltinProviders
import me.liam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
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
