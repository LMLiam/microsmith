package io.github.lmliam.microsmith.maven

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

internal fun interface MicrosmithScriptHostRunner {
    fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult
}
