package io.github.lmliam.microsmith.gen.render

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembly
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.gen.ArtifactRendererRegistry
import io.github.lmliam.microsmith.gen.RunScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@Inject
@SingleIn(RunScope::class)
internal class ArtifactRenderingService(
    private val rendererRegistry: ArtifactRendererRegistry,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun render(assembly: ArtifactAssembly): List<GeneratedFile> =
        withContext(ioDispatcher) {
            assembly
                .artifacts()
                .map { artifact -> async { rendererRegistry.resolve(artifact).run { render(artifact) } } }
                .awaitAll()
        }
}
