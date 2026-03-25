package io.github.lmliam.microsmith.artifact.core

class ArtifactAssembly internal constructor(
    private val artifactsById: LinkedHashMap<ArtifactId<out Artifact>, Artifact>,
) {
    fun artifacts(): List<Artifact> = artifactsById.values.toList()

    operator fun get(artifactId: ArtifactId<out Artifact>): Artifact? = artifactsById[artifactId]
}
