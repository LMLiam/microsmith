package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.artifact.core.ArtifactAssembly
import io.github.lmliam.microsmith.gen.core.ArtifactRendererRegistry
import io.github.lmliam.microsmith.gen.files.GeneratedFile

internal class ArtifactRenderingService(
    private val rendererRegistry: ArtifactRendererRegistry = ArtifactRendererRegistry(),
) {
    fun render(assembly: ArtifactAssembly): List<GeneratedFile> = assembly.artifacts().map { artifact ->
        rendererRegistry.resolve(artifact).run { render(artifact) }
    }
}
