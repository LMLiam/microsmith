package io.github.lmliam.microsmith.sbt.execution

import io.github.lmliam.microsmith.runtime.scripting.MicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

object DefaultMicrosmithSbtScriptHostRunner : MicrosmithSbtScriptHostRunner {
    override fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult =
        MicrosmithScriptHost(cacheDirectory).run(request)
}
