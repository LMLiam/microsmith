package io.github.lmliam.microsmith.artifact.assembly

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactId
import kotlin.reflect.KClass

internal class ArtifactAssemblerRegistry(assemblers: List<ArtifactAssembler<*>>) {
    private val assemblersByType: Map<KClass<out Artifact>, ArtifactAssembler<*>>

    init {
        val grouped = assemblers.groupBy { it.artifactType }
        val duplicateTypes = grouped.filterValues { it.size > 1 }.keys

        require(duplicateTypes.isEmpty()) {
            val types = duplicateTypes.map { it.qualifiedName ?: it.toString() }.sorted().joinToString(", ")

            "Duplicate artifact assemblers registered for artifact type $types"
        }

        assemblersByType = grouped.mapValues { (_, registrations) -> registrations.single() }
    }

    fun resolve(artifactId: ArtifactId<out Artifact>): ArtifactAssembler<Artifact> {
        val assembler =
            assemblersByType[artifactId.artifactType]
                ?: error("No artifact assembler found for artifact type: ${artifactId.artifactType}")

        @Suppress("UNCHECKED_CAST")
        return assembler as ArtifactAssembler<Artifact>
    }
}
