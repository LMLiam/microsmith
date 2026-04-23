package io.github.lmliam.microsmith.gen.files.render

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import java.nio.charset.StandardCharsets

@ServiceProvider(ArtifactRenderer::class)
class TextFileArtifactRenderer : ArtifactRenderer<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

    override fun render(artifact: TextFileArtifact): GeneratedFile {
        val renderedContents = GeneratedByMicrosmithBanner.prepend(artifact.id.relativePath, artifact.contents)
        return GeneratedFile(
            relativePath = artifact.id.relativePath,
            contents = renderedContents.toByteArray(StandardCharsets.UTF_8),
            outputRoot = artifact.id.outputRoot,
            origins = artifact.origins,
        )
    }
}
