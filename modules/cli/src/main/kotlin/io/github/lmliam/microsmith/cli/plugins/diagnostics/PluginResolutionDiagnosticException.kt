package io.github.lmliam.microsmith.cli.plugins.diagnostics
import io.github.lmliam.microsmith.cli.plugins.PluginResolverErrorCategory
internal class PluginResolutionDiagnosticException(
    val category: PluginResolverErrorCategory,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
