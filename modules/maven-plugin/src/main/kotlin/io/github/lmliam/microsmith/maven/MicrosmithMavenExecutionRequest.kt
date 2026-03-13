package io.github.lmliam.microsmith.maven

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

internal data class MicrosmithMavenExecutionRequest(
    val scriptRunRequest: ScriptRunRequest,
    val outputDirectory: Path,
    val cacheDirectory: Path,
)
