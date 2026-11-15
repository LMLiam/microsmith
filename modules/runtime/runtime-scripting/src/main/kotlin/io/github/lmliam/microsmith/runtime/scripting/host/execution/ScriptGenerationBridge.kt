package io.github.lmliam.microsmith.runtime.scripting.host.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.MicrosmithGenerator
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

@Inject
internal class ScriptGenerationBridge(
    private val generator: MicrosmithGenerator,
    private val request: ScriptRunRequest,
    private val classLoader: ScriptRunClassLoader,
) {
    fun generate(model: MicrosmithModel): List<Path> = runBlocking {
        generator.generateTo(model, request.outputDir, classLoader.loader)
    }
}
