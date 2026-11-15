package io.github.lmliam.microsmith.maven.execution

import io.github.lmliam.microsmith.runtime.scripting.createSystemMicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

internal object DefaultMicrosmithScriptHostRunner : MicrosmithScriptHostRunner {
    override fun run(cacheDirectory: Path, request: ScriptRunRequest): ScriptRunResult = runBlocking {
        createSystemMicrosmithScriptHost(cacheDirectory).run(request)
    }
}
