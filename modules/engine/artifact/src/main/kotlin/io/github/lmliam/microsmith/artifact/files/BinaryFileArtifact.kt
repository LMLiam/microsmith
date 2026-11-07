package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.Artifact

class BinaryFileArtifact(
    override val id: BinaryFileArtifactId,
    contents: ByteArray,
) : Artifact {
    private val bytes: ByteArray = contents.copyOf()

    val contents: ByteArray
        get() = bytes.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryFileArtifact) return false

        if (id != other.id) return false
        return bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }

    override fun toString(): String = "BinaryFileArtifact(id=$id, size=${bytes.size} bytes)"
}
