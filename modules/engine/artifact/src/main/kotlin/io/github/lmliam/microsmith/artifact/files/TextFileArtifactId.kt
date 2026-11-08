package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.ArtifactId
import java.nio.file.Path

private val DEFAULT_OUTPUT_ROOT: Path = Path.of(".")

data class TextFileArtifactId(
    val relativePath: Path,
    val outputRoot: Path = DEFAULT_OUTPUT_ROOT,
) : ArtifactId<TextFileArtifact> {
    init {
        require(!relativePath.isAbsolute) { "Relative path '$relativePath' must not be absolute." }
    }

    override val artifactType = TextFileArtifact::class
}
