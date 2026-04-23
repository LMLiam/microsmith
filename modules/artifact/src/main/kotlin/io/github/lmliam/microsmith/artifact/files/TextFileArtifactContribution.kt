package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

data class TextFileArtifactContribution(
    override val artifactId: TextFileArtifactId,
    val contents: String,
    val origins: Set<String> = emptySet(),
) : ArtifactContribution<TextFileArtifact>
