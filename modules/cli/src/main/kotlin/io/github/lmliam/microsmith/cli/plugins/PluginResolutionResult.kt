package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal sealed interface PluginResolutionResult {
    data class Success(val classpath: List<Path>, val lockfilePath: Path?) : PluginResolutionResult

    data class Failure(val diagnostics: List<String>) : PluginResolutionResult
}
