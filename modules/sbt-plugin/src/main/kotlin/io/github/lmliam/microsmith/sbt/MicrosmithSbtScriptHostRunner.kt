package io.github.lmliam.microsmith.sbt

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

fun interface MicrosmithSbtScriptHostRunner {
    fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult
}
