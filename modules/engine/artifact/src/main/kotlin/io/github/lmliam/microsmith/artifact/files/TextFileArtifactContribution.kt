package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.ArtifactContribution

public data class TextFileArtifactContribution(
    override val artifactId: TextFileArtifactId,
    public val contents: String,
) : ArtifactContribution<TextFileArtifact>
