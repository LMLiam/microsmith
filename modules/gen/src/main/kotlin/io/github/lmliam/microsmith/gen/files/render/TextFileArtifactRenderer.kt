package io.github.lmliam.microsmith.gen.files.render

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@ServiceProvider(ArtifactRenderer::class)
class TextFileArtifactRenderer : ArtifactRenderer<TextFileArtifact> {
    override val artifactType: KClass<TextFileArtifact> = TextFileArtifact::class

    override fun render(artifact: TextFileArtifact): GeneratedFile {
        return GeneratedFile(
            relativePath = artifact.id.relativePath,
            contents = artifact.contents.toByteArray(StandardCharsets.UTF_8),
            outputRoot = artifact.id.outputRoot,
        )
    }
}
