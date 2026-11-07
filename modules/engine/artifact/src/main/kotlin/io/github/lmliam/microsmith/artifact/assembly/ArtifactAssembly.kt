package io.github.lmliam.microsmith.artifact.assembly

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactId

class ArtifactAssembly internal constructor(artifactsById: Map<ArtifactId<Artifact>, Artifact>) {
    private val artifactsById = artifactsById.toMap()

    fun artifacts(): List<Artifact> = artifactsById.values.toList()

    operator fun get(artifactId: ArtifactId<Artifact>): Artifact? = artifactsById[artifactId]
}
