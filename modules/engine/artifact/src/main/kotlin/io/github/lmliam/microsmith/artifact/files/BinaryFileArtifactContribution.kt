package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.ArtifactContribution

class BinaryFileArtifactContribution(
    override val artifactId: BinaryFileArtifactId,
    contents: ByteArray,
) : ArtifactContribution<BinaryFileArtifact> {
    private val bytes: ByteArray = contents.copyOf()

    val contents: ByteArray
        get() = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryFileArtifactContribution) return false

        if (artifactId != other.artifactId) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = artifactId.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    override fun toString(): String = "BinaryFileArtifactContribution(artifactId=$artifactId, size=${bytes.size} bytes)"
}
