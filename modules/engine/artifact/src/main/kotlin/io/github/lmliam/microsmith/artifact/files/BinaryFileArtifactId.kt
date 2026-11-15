package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.reflect.KClass

public data class BinaryFileArtifactId(
    val relativePath: RelativePath,
    val outputRoot: RelativePath = RelativePath.ROOT,
) : ArtifactId<BinaryFileArtifact> {
    init {
        require(!relativePath.isRoot) { "Relative path must name a file." }
    }

    override val artifactType: KClass<BinaryFileArtifact> = BinaryFileArtifact::class
}
