package me.liam.microsmith.sbt

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

fun interface MicrosmithSbtScriptHostRunner {
    fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult
}
