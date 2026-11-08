package io.github.lmliam.microsmith.artifact.assembly

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactId

class ArtifactAssemblyService internal constructor(private val assemblerRegistry: ArtifactAssemblerRegistry) {
    constructor(assemblers: List<ArtifactAssembler<*>>) : this(ArtifactAssemblerRegistry(assemblers))

    fun assemble(contributions: List<ArtifactContribution<Artifact>>): ArtifactAssembly =
        assembleRetaining(
            retainedArtifacts = emptyList(),
            contributions = contributions,
        )

    fun assembleRetaining(
        retainedArtifacts: List<Artifact>,
        contributions: List<ArtifactContribution<Artifact>>,
    ): ArtifactAssembly {
        val artifactsById = linkedMapOf<ArtifactId<Artifact>, Artifact>()

        for (artifact in retainedArtifacts) {
            val previous = artifactsById.put(artifact.id, artifact)
            require(previous == null || previous == artifact) { "Conflicting retained artifact for '${artifact.id}'." }
        }

        for (contribution in contributions) {
            artifactsById.applyContribution(contribution)
        }

        return ArtifactAssembly(artifactsById)
    }

    private fun <T : Artifact> MutableMap<ArtifactId<Artifact>, Artifact>.applyContribution(
        contribution: ArtifactContribution<T>
    ) {
        val artifactId = contribution.artifactId
        val assembler = assemblerRegistry.resolve(artifactId)

        @Suppress("UNCHECKED_CAST") val current = get(artifactId) as T?

        put(
            artifactId,
            if (current == null) assembler.create(contribution) else assembler.merge(current, contribution),
        )
    }
}
