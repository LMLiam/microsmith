package me.liam.microsmith.maven

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

internal fun interface MicrosmithScriptHostRunner {
    fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult
}
