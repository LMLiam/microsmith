package io.github.lmliam.microsmith.gen

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.composition.RunGraph
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.Path
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
internal class RunGraphMicrosmithGenerator(private val runGraphFactory: RunGraph.Factory) : MicrosmithGenerator {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun generate(
        model: MicrosmithModel,
        finalDir: FileSpace,
        pluginClassLoader: ClassLoader,
    ): List<Path> {
        val runGraph = runGraphFactory.create(pluginClassLoader)
        val plugins = runGraph.plugins
        var failure: Throwable? = null

        try {
            return runGraph.runner.generate(model, finalDir)
        } catch (thrown: Throwable) {
            failure = thrown
            throw thrown
        } finally {
            withContext(NonCancellable) { plugins.close(failure) }
        }
    }

    override suspend fun generateTo(
        model: MicrosmithModel,
        outputDir: Path,
        pluginClassLoader: ClassLoader,
    ): List<Path> = generate(model, DirectorySpace(outputDir), pluginClassLoader)
}
