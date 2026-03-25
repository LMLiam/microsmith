package io.github.lmliam.microsmith.artifact.core

class ArtifactAssemblyService {
    private val assemblerRegistry: ArtifactAssemblerRegistry

    constructor() {
        assemblerRegistry = ArtifactAssemblerRegistry()
    }

    internal constructor(assemblerRegistry: ArtifactAssemblerRegistry) {
        this.assemblerRegistry = assemblerRegistry
    }

    fun assemble(contributions: List<ArtifactContribution<out Artifact>>): ArtifactAssembly {
        val artifacts = linkedMapOf<ArtifactId<out Artifact>, Artifact>()

        contributions.forEach { contribution ->
            val assembler = assemblerRegistry.resolve(contribution.artifactId)
            val current = artifacts[contribution.artifactId]
            artifacts[contribution.artifactId] =
                if (current == null) {
                    assembler.create(contribution.cast())
                } else {
                    assembler.merge(current, contribution.cast())
                }
        }

        return ArtifactAssembly(LinkedHashMap(artifacts))
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactContribution<out Artifact>.cast(): ArtifactContribution<Artifact> {
        return this as ArtifactContribution<Artifact>
    }
}
