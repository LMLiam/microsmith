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
            val formattedTypes = duplicateTypes.map { it.displayName }.sorted().joinToString(", ")
            "Duplicate artifact assemblers registered for artifact type(s): $formattedTypes"
        }

        assemblersByType = grouped.mapValues { (_, registrations) -> registrations.single() }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Artifact> resolve(artifactId: ArtifactId<T>): ArtifactAssembler<T> {
        val assembler =
            assemblersByType[artifactId.artifactType]
                ?: error("No artifact assembler registered for artifact type: ${artifactId.artifactType.displayName}")

        return assembler as ArtifactAssembler<T>
    }
}

private val KClass<*>.displayName: String
    get() = qualifiedName ?: simpleName ?: toString()
