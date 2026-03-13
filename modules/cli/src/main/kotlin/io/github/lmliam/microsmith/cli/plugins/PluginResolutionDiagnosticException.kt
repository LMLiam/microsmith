package io.github.lmliam.microsmith.cli.plugins

internal class PluginResolutionDiagnosticException(
    val category: PluginResolverErrorCategory,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
