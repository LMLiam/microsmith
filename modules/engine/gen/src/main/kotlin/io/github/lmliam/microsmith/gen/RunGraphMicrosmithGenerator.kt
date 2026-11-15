package io.github.lmliam.microsmith.gen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.composition.RunGraph
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.Path

@Inject
@SingleIn(AppScope::class)
internal class RunGraphMicrosmithGenerator(private val runGraphFactory: RunGraph.Factory) : MicrosmithGenerator {
    override suspend fun generate(
        model: MicrosmithModel,
        finalDir: FileSpace,
        pluginClassLoader: ClassLoader,
    ): List<Path> = runGraphFactory.create(pluginClassLoader).runner.generate(model, finalDir)

    override suspend fun generateTo(
        model: MicrosmithModel,
        outputDir: Path,
        pluginClassLoader: ClassLoader,
    ): List<Path> = generate(model, DirectorySpace(outputDir), pluginClassLoader)
}
