package io.github.lmliam.microsmith.gen.files.render

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.ArtifactRenderer
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import java.nio.charset.StandardCharsets

@Inject
internal class TextFileArtifactRenderer(private val banner: GeneratedByMicrosmithBanner) :
    ArtifactRenderer<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

    override fun render(artifact: TextFileArtifact): GeneratedFile {
        val renderedContents = banner.prepend(artifact.id.relativePath, artifact.contents)
        return GeneratedFile(
            relativePath = artifact.id.relativePath,
            contents = renderedContents.toByteArray(StandardCharsets.UTF_8),
            outputRoot = artifact.id.outputRoot,
        )
    }
}
