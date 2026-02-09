package me.liam.microsmith.cli

import me.liam.microsmith.cli.command.CliCommand
import me.liam.microsmith.cli.command.ErrorCommand
import me.liam.microsmith.cli.command.HelpCommand
import me.liam.microsmith.cli.command.RunCommand
import me.liam.microsmith.cli.parsing.parseCliArgs
import me.liam.microsmith.cli.provider.verifyBuiltinProviders
import java.util.ServiceConfigurationError

class MicrosmithCli(
    private val stdout: (String) -> Unit = ::println,
    private val stderr: (String) -> Unit = { System.err.println(it) },
    private val providerValidator: () -> List<String> = ::verifyBuiltinProviders
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

        if (providerErrors.isEmpty()) {
            stdout(
                "Phase 1 scaffold complete. " +
                    "Script execution will be added in Phase 2. " +
                    "script='${command.script}', out='${command.outputDir}'."
            )
        } else {
            providerErrors.forEach(stderr)
        }

        return if (providerErrors.isEmpty()) 0 else 2
    }

    private fun collectProviderErrors(): List<String> =
        try {
            providerValidator()
        } catch (error: ServiceConfigurationError) {
            val message = error.message ?: error::class.simpleName ?: "ServiceConfigurationError"
            listOf("Failed to load runtime service providers: $message")
        }
}
