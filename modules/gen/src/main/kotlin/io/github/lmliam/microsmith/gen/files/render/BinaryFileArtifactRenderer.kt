package io.github.lmliam.microsmith.gen.files.render

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.files.BinaryFileArtifact
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile

@ServiceProvider(ArtifactRenderer::class)
class BinaryFileArtifactRenderer : ArtifactRenderer<BinaryFileArtifact> {
    override val artifactType = BinaryFileArtifact::class

    override fun render(artifact: BinaryFileArtifact): GeneratedFile {
        return GeneratedFile(
            relativePath = artifact.id.relativePath,
            contents = artifact.copiedContents,
            outputRoot = artifact.id.outputRoot,
        )
    }
}
