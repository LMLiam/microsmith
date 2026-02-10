package me.liam.microsmith.cli

import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.parsing.parseCliArgs
import me.liam.microsmith.cli.provider.verifyBuiltinProviders
import me.liam.microsmith.runtime.scripting.MicrosmithScriptHost
import me.liam.microsmith.runtime.scripting.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.ScriptRunResult
import me.liam.microsmith.runtime.scripting.ScriptRunSuccess
import java.util.ServiceConfigurationError

internal class MicrosmithCli(
    private val stdout: (String) -> Unit = ::println,
    private val stderr: (String) -> Unit = { System.err.println(it) },
    private val providerValidator: () -> List<String> = ::verifyBuiltinProviders,
    private val scriptRunner: (RunCommand) -> ScriptRunResult = { command ->
        MicrosmithScriptHost().run(
            ScriptRunRequest(
                script = command.script,
                outputDir = command.outputDir,
                variables = command.variables,
                flags = command.flags
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

        if (providerErrors.isNotEmpty()) {
            providerErrors.forEach(stderr)
            return 2
        }

        return when (val result = scriptRunner(command)) {
            is ScriptRunSuccess -> {
                result.warnings.forEach(stderr)
                val cacheState = if (result.cacheHit) "hit" else "miss"
                stdout(
                    "Generated script '${command.script}' into '${command.outputDir}' " +
                        "(compile-cache=$cacheState, elapsed=${result.elapsedMillis}ms)."
                )
                0
            }
            is ScriptRunFailure -> {
                result.diagnostics.forEach(stderr)
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
