package me.liam.microsmith.cli

import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.parsing.parseCliArgs
import me.liam.microsmith.cli.plugins.PluginResolutionResult
import me.liam.microsmith.cli.plugins.resolvePlugins
import me.liam.microsmith.cli.provider.verifyBuiltinProviders
import me.liam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Path
import java.util.ServiceConfigurationError

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
                pluginClasspath = pluginClasspath
            )
        )
    }
) {
    fun run(args: Array<String>): Int =
        when (val parsed = parseCliArgs(args.toList())) {
            is HelpCommand -> {
                stdout(HELP_TEXT.trimIndent())
                0
            }
            is ErrorCommand -> {
                stderr(parsed.message)
                stderr("")
                stderr(HELP_TEXT.trimIndent())
                2
            }
            is RunCommand -> runCommand(parsed)
        }

    private fun runCommand(command: RunCommand): Int {
        val providerErrors = collectProviderErrors()
        val runResult =
            if (providerErrors.isNotEmpty()) {
                providerErrors.forEach(stderr)
                null
            } else {
                when (val resolvedPlugins = pluginResolver(command)) {
                    is PluginResolutionResult.Failure -> {
                        resolvedPlugins.diagnostics.forEach(stderr)
                        null
                    }
                    is PluginResolutionResult.Success -> scriptRunner(command, resolvedPlugins.classpath)
                }
            }

        return when (runResult) {
            null -> 2
            is ScriptRunSuccess -> {
                runResult.warnings.forEach(stderr)
                val cacheState = if (runResult.cacheHit) "hit" else "miss"
                stdout(
                    "Generated script '${command.script}' into '${command.outputDir}' " +
                        "(compile-cache=$cacheState, elapsed=${runResult.elapsedMillis}ms)."
                )
                0
            }
            is ScriptRunFailure -> {
                runResult.diagnostics.forEach(stderr)
                2
            }
        }
    }

    private fun collectProviderErrors(): List<String> =
        try {
            providerValidator()
        } catch (error: ServiceConfigurationError) {
            val message = error.message ?: error::class.simpleName ?: "ServiceConfigurationError"
            listOf("Failed to load runtime service providers: $message")
        }
}
