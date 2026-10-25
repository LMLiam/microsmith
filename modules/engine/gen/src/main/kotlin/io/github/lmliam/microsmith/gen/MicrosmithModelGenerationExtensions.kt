package io.github.lmliam.microsmith.gen

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.composition.MicrosmithGenerationGraph
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPlugins
import java.nio.file.Path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun MicrosmithModel.generate(finalDir: FileSpace): List<Path> {
    val generationGraph = createGraphFactory<MicrosmithGenerationGraph.Factory>().create(discoverMicrosmithPlugins())

    return generationGraph.runner.generate(model = this, finalDir = finalDir)
}

suspend fun MicrosmithModel.generateTo(
    outputDir: Path,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): List<Path> {
    val directorySpace = withContext(ioDispatcher) { DirectorySpace.from(outputDir) }

    return generate(directorySpace)
}
