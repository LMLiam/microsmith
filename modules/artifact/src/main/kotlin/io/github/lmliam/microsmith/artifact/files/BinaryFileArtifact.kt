package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.Artifact

class BinaryFileArtifact(override val id: BinaryFileArtifactId, contents: ByteArray) : Artifact {
    private val bytes = contents.copyOf()

    val copiedContents: ByteArray
        get() = bytes.copyOf()
}
