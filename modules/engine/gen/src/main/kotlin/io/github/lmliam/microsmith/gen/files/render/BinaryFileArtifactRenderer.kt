package io.github.lmliam.microsmith.gen.files.render

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.ArtifactRenderer
import io.github.lmliam.microsmith.artifact.files.BinaryFileArtifact
import io.github.lmliam.microsmith.artifact.files.GeneratedFile

@Inject
internal class BinaryFileArtifactRenderer : ArtifactRenderer<BinaryFileArtifact> {
    override val artifactType = BinaryFileArtifact::class

    override fun render(artifact: BinaryFileArtifact): GeneratedFile =
        GeneratedFile(
            relativePath = artifact.id.relativePath,
            contents = artifact.contents,
            outputRoot = artifact.id.outputRoot,
        )
}
