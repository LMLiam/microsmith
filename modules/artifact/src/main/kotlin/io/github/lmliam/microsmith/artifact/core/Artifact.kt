package io.github.lmliam.microsmith.artifact.core

interface Artifact {
    val id: ArtifactId<out Artifact>
}
