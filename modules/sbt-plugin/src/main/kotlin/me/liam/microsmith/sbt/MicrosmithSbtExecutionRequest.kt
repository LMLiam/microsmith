package me.liam.microsmith.sbt

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

data class MicrosmithSbtExecutionRequest(
    val scriptRunRequest: ScriptRunRequest,
    val outputDirectory: Path,
    val cacheDirectory: Path,
)
