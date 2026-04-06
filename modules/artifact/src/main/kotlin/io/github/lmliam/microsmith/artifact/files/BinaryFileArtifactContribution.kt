package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

class BinaryFileArtifactContribution(override val artifactId: BinaryFileArtifactId, contents: ByteArray) :
    ArtifactContribution<BinaryFileArtifact> {
    private val bytes = contents.copyOf()

    val copiedContents: ByteArray
        get() = bytes.copyOf()
}
