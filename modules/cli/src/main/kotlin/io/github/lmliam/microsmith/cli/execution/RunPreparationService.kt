package io.github.lmliam.microsmith.cli.execution

import io.github.lmliam.microsmith.cli.command.RunCommand
import io.github.lmliam.microsmith.cli.diagnostics.CliDiagnosticEmitter
import io.github.lmliam.microsmith.cli.diagnostics.CliFailureCode
import io.github.lmliam.microsmith.cli.plugins.PluginResolutionResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path
import java.util.ServiceConfigurationError

internal class RunPreparationService(
    private val providerValidator: () -> List<String>,
    private val pluginResolver: (RunCommand) -> PluginResolutionResult,
    private val scriptRunner: (RunCommand, List<Path>) -> ScriptRunResult,
) {
    fun prepare(command: RunCommand, emitter: CliDiagnosticEmitter, context: RunExecutionContext): PreparedRun {
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
                context.resolverStatus = RunExecutionStatus.FAILURE
                emitter.error(
                    CliFailureCode.PLUGIN_RESOLUTION_FAILED,
                    "[unexpected] Plugin resolution failed unexpectedly.",
                    details = mapOf("exceptionType" to (error::class.simpleName ?: "unknown")),
                )
                return PreparedRun.Failure(CliFailureCode.PLUGIN_RESOLUTION_FAILED)
            }

        return when (resolvedPlugins) {
            is PluginResolutionResult.Failure -> {
                context.resolverStatus = RunExecutionStatus.FAILURE
                resolvedPlugins.diagnostics.forEach { diagnostic ->
                    emitter.error(CliFailureCode.PLUGIN_RESOLUTION_FAILED, diagnostic)
                }
                PreparedRun.Failure(CliFailureCode.PLUGIN_RESOLUTION_FAILED)
            }

            is PluginResolutionResult.Success -> {
                context.resolverStatus = RunExecutionStatus.SUCCESS
                context.lockfilePath = resolvedPlugins.lockfilePath
                PreparedRun.Ready(scriptRunner(command, resolvedPlugins.classpath))
            }
        }
    }

    private fun collectProviderErrors(): List<String> = try {
        providerValidator()
    } catch (error: ServiceConfigurationError) {
        val message = error.message ?: error::class.simpleName ?: "ServiceConfigurationError"
        listOf("Failed to load runtime service providers: $message")
    }
}
