package me.liam.microsmith.maven

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

internal data class MicrosmithMavenExecutionRequest(
    val scriptRunRequest: ScriptRunRequest,
    val outputDirectory: Path,
    val cacheDirectory: Path,
)
