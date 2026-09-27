package io.github.lmliam.microsmith.gen.render
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembly
import io.github.lmliam.microsmith.gen.ArtifactRendererRegistry
import io.github.lmliam.microsmith.gen.MicrosmithGenerationScope
import io.github.lmliam.microsmith.gen.files.GeneratedFile

@Inject
@SingleIn(MicrosmithGenerationScope::class)
internal class ArtifactRenderingService(private val rendererRegistry: ArtifactRendererRegistry) {
    fun render(assembly: ArtifactAssembly): List<GeneratedFile> = assembly.artifacts()
        .map { artifact ->
            rendererRegistry
                .resolve(artifact)
                .run { render(artifact) }
        }
}
