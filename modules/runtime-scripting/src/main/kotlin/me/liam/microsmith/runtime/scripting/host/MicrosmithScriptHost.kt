package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
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
