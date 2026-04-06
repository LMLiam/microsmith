package io.github.lmliam.microsmith.artifact.core

import java.util.ServiceLoader
import kotlin.reflect.KClass

internal class ArtifactAssemblerRegistry(assemblers: List<ArtifactAssembler<*>> = loadArtifactAssemblers()) {
    private val assemblersByType = indexAssemblers(assemblers)

    fun resolve(artifactId: ArtifactId<out Artifact>): ArtifactAssembler<Artifact> =
        assemblersByType[artifactId.artifactType]
            ?.cast()
            ?: error(
                "No artifact assembler found for artifact type: ${artifactId.artifactType}",
            )

    private fun indexAssemblers(
        assemblers: List<ArtifactAssembler<*>>,
    ): Map<KClass<out Artifact>, ArtifactAssembler<*>> {
        val duplicates =
            assemblers
                .groupBy(ArtifactAssembler<*>::artifactType)
                .filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate artifact assemblers registered for artifact types: $types"
        }

        return assemblers.associateBy(ArtifactAssembler<*>::artifactType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactAssembler<*>.cast(): ArtifactAssembler<Artifact> = this as ArtifactAssembler<Artifact>

    private fun formatType(type: KClass<out Artifact>): String = type.qualifiedName ?: type.toString()
}

private fun loadArtifactAssemblers(): List<ArtifactAssembler<*>> =
    ServiceLoader.load(ArtifactAssembler::class.java).iterator().asSequence().toList()
