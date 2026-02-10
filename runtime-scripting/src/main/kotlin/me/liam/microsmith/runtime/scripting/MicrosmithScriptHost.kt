package me.liam.microsmith.runtime.scripting

import java.nio.file.Path

class MicrosmithScriptHost(
    private val cacheDirectory: Path = ScriptHostPaths.defaultCacheDirectory()
) {
    private val runExecutor = ScriptRunExecutor(cacheDirectory)

    fun run(request: ScriptRunRequest): ScriptRunResult {
        val scriptPath = request.script.toAbsolutePath().normalize()
        val outputPath = request.outputDir.toAbsolutePath().normalize()
        val validationFailure = ScriptPathValidator.validate(scriptPath)
        return validationFailure ?: runExecutor.execute(request, scriptPath, outputPath)
    }
}
