package io.github.lmliam.microsmith.artifact.core

class ArtifactAssemblyService {
    private val assemblerRegistry: ArtifactAssemblerRegistry

    constructor() {
        assemblerRegistry = ArtifactAssemblerRegistry()
    }

    constructor(assemblers: List<ArtifactAssembler<*>>) {
        assemblerRegistry = ArtifactAssemblerRegistry(assemblers)
    }

    internal constructor(assemblerRegistry: ArtifactAssemblerRegistry) {
        this.assemblerRegistry = assemblerRegistry
    }

    fun assemble(contributions: List<ArtifactContribution<out Artifact>>): ArtifactAssembly {
        return assembleRetaining(emptyList(), contributions)
    }

    fun assembleRetaining(
        retainedArtifacts: List<Artifact>,
        contributions: List<ArtifactContribution<out Artifact>>,
    ): ArtifactAssembly {
        val assembled = linkedMapOf<ArtifactId<out Artifact>, Artifact>()
        retainedArtifacts.forEach { artifact ->
            val previous = assembled.put(artifact.id, artifact)
            require(previous == null || previous == artifact) {
                "Conflicting retained artifact for '${artifact.id}'."
            }
        }

        val artifactsById = linkedMapOf<ArtifactId<out Artifact>, Artifact>()
        artifactsById.putAll(assembled)

        contributions.forEach { contribution ->
            val assembler = assemblerRegistry.resolve(contribution.artifactId)
            val current = artifactsById[contribution.artifactId]
            artifactsById[contribution.artifactId] =
                if (current == null) {
                    assembler.create(contribution.cast())
                } else {
                    assembler.merge(current, contribution.cast())
                }
        }

        return ArtifactAssembly(LinkedHashMap(artifactsById))
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactContribution<out Artifact>.cast(): ArtifactContribution<Artifact> {
        return this as ArtifactContribution<Artifact>
    }
}
