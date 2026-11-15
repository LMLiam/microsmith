package io.github.lmliam.microsmith.artifact.assembly

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactId

@InternalMicrosmithApi
public class ArtifactAssembly internal constructor(artifactsById: Map<ArtifactId<Artifact>, Artifact>) {
    private val artifactsById = artifactsById.toMap()

    public fun artifacts(): List<Artifact> = artifactsById.values.toList()

    public operator fun get(artifactId: ArtifactId<Artifact>): Artifact? = artifactsById[artifactId]
}
