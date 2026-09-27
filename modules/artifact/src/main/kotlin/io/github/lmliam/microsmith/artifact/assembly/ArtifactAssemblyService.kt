package io.github.lmliam.microsmith.artifact.assembly

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.collections.emptyList

class ArtifactAssemblyService(assemblers: List<ArtifactAssembler<*>>) {
    private val assemblerRegistry = ArtifactAssemblerRegistry(assemblers)

    fun assemble(contributions: List<ArtifactContribution<out Artifact>>): ArtifactAssembly = assembleRetaining(
        retainedArtifacts = emptyList(),
        contributions = contributions,
    )

    fun assembleRetaining(
        retainedArtifacts: List<Artifact>,
        contributions: List<ArtifactContribution<out Artifact>>,
    ): ArtifactAssembly {
        val artifactsById = linkedMapOf<ArtifactId<out Artifact>, Artifact>()

        for (artifact in retainedArtifacts) {
            val previous = artifactsById.put(artifact.id, artifact)

            require(previous == null || previous == artifact) {
                "Conflicting retained artifact for '${artifact.id}'"
            }
        }

        for (contribution in contributions) {
            val artifactId = contribution.artifactId
            val assembler = assemblerRegistry.resolve(artifactId)
            val current = artifactsById[artifactId]

            @Suppress("UNCHECKED_CAST")
            val typedContribution = contribution as ArtifactContribution<Artifact>

            artifactsById[artifactId] =
                if (current == null) {
                    assembler.create(typedContribution)
                } else {
                    assembler.merge(current, typedContribution)
                }
        }

        return ArtifactAssembly(artifactsById)
    }
}
