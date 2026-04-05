package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import java.nio.file.Path

private val defaultBinaryFileOutputRoot: Path = Path.of(".")

data class BinaryFileArtifactId(
    val relativePath: Path,
    val outputRoot: Path = defaultBinaryFileOutputRoot,
) : ArtifactId<BinaryFileArtifact> {
    override val artifactType = BinaryFileArtifact::class
}
