package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.ArtifactId

data class TextFileArtifactId(val relativePath: RelativePath, val outputRoot: RelativePath = RelativePath.ROOT) :
    ArtifactId<TextFileArtifact> {
    init {
        require(!relativePath.isRoot) { "Relative path must name a file." }
    }

    override val artifactType = TextFileArtifact::class
}
