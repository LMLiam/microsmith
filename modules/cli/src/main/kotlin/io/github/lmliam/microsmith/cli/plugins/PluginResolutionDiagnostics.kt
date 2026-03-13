package io.github.lmliam.microsmith.cli.plugins

internal class PluginResolutionDiagnostics {
    fun format(error: Throwable, sensitiveValues: Set<String>): String = when (error) {
        is PluginResolutionDiagnosticException ->
            "[${error.category.code}] " +
                (error.message ?: "plugin resolution failed")
                    .redactSensitiveValues(sensitiveValues)

        else -> {
            val unexpectedMessage = error.message ?: error::class.simpleName ?: "unknown plugin resolution error"
            "[unexpected] ${unexpectedMessage.redactSensitiveValues(sensitiveValues)}"
        }
    }
}
