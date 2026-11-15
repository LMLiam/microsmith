package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.Artifact

public data class TextFileArtifact(
    override val id: TextFileArtifactId,
    public val contents: String,
) : Artifact
