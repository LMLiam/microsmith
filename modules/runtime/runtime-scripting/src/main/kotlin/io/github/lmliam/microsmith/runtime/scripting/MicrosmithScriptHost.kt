package io.github.lmliam.microsmith.runtime.scripting

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.host.execution.ScriptRunExecutor
import io.github.lmliam.microsmith.runtime.scripting.host.paths.ScriptPathValidator
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult

@Inject
public class MicrosmithScriptHost
internal constructor(
    private val runExecutor: ScriptRunExecutor,
    private val pathValidator: ScriptPathValidator,
) {
    public suspend fun run(request: ScriptRunRequest): ScriptRunResult {
        val normalizedRequest = request.normalized()
        val validationFailure = pathValidator.validate(normalizedRequest.script)
        return validationFailure ?: runExecutor.execute(normalizedRequest)
    }
}

private fun ScriptRunRequest.normalized(): ScriptRunRequest =
    copy(
        script = script.toAbsolutePath().normalize(),
        outputDir = outputDir.toAbsolutePath().normalize(),
        pluginClasspath = pluginClasspath.map { it.toAbsolutePath().normalize() },
    )
