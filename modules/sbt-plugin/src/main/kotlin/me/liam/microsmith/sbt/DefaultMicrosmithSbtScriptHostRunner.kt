package me.liam.microsmith.sbt

import me.liam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

object DefaultMicrosmithSbtScriptHostRunner : MicrosmithSbtScriptHostRunner {
    override fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult =
        MicrosmithScriptHost(cacheDirectory).run(request)
}
