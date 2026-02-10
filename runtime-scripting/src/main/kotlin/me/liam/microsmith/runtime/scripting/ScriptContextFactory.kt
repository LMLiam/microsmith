package me.liam.microsmith.runtime.scripting

import kotlinx.coroutines.runBlocking
import me.liam.microsmith.gen.helpers.generateTo
import java.nio.file.Path

internal object ScriptContextFactory {
    fun create(
        outputPath: Path,
        request: ScriptRunRequest
    ): MicrosmithScriptContext =
        MicrosmithScriptContext(
            outDir = outputPath,
            vars = request.variables,
            flags = request.flags
        ) { model ->
            runBlocking {
                model.generateTo(outputPath)
            }
        }
}
