package io.github.lmliam.microsmith.artifact.files

import io.github.lmliam.microsmith.artifact.core.Artifact

data class TextFileArtifact(
    override val id: TextFileArtifactId,
    val contents: String,
) : Artifact
