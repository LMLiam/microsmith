package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.gen.helpers.generateTo
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

internal object ScriptContextFactory {
    fun create(outputPath: Path, request: ScriptRunRequest): MicrosmithScriptContext = MicrosmithScriptContext(
        outDir = outputPath,
        vars = request.variables,
        flags = request.flags,
    ) { model ->
        runBlocking {
            model.generateTo(outputPath)
        }
    }
}
