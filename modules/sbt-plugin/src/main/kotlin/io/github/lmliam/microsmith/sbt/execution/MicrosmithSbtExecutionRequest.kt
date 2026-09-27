package io.github.lmliam.microsmith.sbt.execution

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

data class MicrosmithSbtExecutionRequest(
    val scriptRunRequest: ScriptRunRequest,
    val outputDirectory: Path,
    val cacheDirectory: Path,
)
