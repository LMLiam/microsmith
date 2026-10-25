package io.github.lmliam.microsmith.artifact

interface Artifact {
    val id: ArtifactId<out Artifact>
}
