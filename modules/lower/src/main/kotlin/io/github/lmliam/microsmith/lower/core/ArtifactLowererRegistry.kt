package io.github.lmliam.microsmith.lower.core

import io.github.lmliam.microsmith.artifact.core.Artifact
import java.util.ServiceLoader
import kotlin.reflect.KClass

internal class ArtifactLowererRegistry(
    lowerers: List<ArtifactLowerer<*>> = loadArtifactLowerers(),
) {
    private val lowerersByType: Map<KClass<out Artifact>, ArtifactLowerer<*>> = indexLowerers(lowerers)

    fun resolveOrNull(artifact: Artifact): ArtifactLowerer<Artifact>? = lowerersByType[artifact.id.artifactType]?.cast()

    private fun indexLowerers(lowerers: List<ArtifactLowerer<*>>): Map<KClass<out Artifact>, ArtifactLowerer<*>> {
        val duplicates = lowerers.groupBy(ArtifactLowerer<*>::artifactType).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate artifact lowerers registered for artifact types: $types"
        }

        return lowerers.associateBy(ArtifactLowerer<*>::artifactType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactLowerer<*>.cast(): ArtifactLowerer<Artifact> = this as ArtifactLowerer<Artifact>

    private fun formatType(type: KClass<out Artifact>): String = type.qualifiedName ?: type.toString()
}

private fun loadArtifactLowerers(): List<ArtifactLowerer<*>> = ServiceLoader.load(ArtifactLowerer::class.java)
    .iterator()
    .asSequence()
    .toList()
