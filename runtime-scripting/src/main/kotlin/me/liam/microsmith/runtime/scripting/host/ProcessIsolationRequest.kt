package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

internal data class ProcessIsolationRequest(
    val request: ScriptRunRequest,
    val scriptPath: Path,
    val outputPath: Path,
    val cacheDirectory: Path,
)
