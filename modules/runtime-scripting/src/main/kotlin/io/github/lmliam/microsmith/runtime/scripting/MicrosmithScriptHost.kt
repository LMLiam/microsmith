package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.runtime.scripting.host.execution.ScriptRunExecutor
import io.github.lmliam.microsmith.runtime.scripting.host.paths.ScriptHostPaths
import io.github.lmliam.microsmith.runtime.scripting.host.paths.ScriptPathValidator
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path

class MicrosmithScriptHost(cacheDirectory: Path = ScriptHostPaths.defaultCacheDirectory()) {
    private val runExecutor = ScriptRunExecutor(cacheDirectory)

    fun run(request: ScriptRunRequest): ScriptRunResult {
        val scriptPath = request.script.toAbsolutePath().normalize()
        val outputPath = request.outputDir.toAbsolutePath().normalize()
        val validationFailure = ScriptPathValidator.validate(scriptPath)
        return validationFailure ?: runExecutor.execute(request, scriptPath, outputPath)
    }
}
