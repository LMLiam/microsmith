package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.artifact.Artifact
import kotlin.reflect.KClass

internal class ArtifactRendererRegistry(renderers: List<ArtifactRenderer<*>>) {
    private val renderersByType = indexRenderers(renderers)

    fun resolve(artifact: Artifact): ArtifactRenderer<Artifact> = renderersByType[artifact.id.artifactType]
        ?.cast()
        ?: error("No artifact renderer found for artifact type: ${artifact.id.artifactType}")

    private fun indexRenderers(renderers: List<ArtifactRenderer<*>>): Map<KClass<out Artifact>, ArtifactRenderer<*>> {
        val duplicates = renderers
            .groupBy(ArtifactRenderer<*>::artifactType)
            .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val types = duplicates.keys
                .map(::formatType)
                .sorted()
                .joinToString(", ")

            "Duplicate artifact renderers registered for artifact types: $types"
        }

        return renderers.associateBy(ArtifactRenderer<*>::artifactType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactRenderer<*>.cast(): ArtifactRenderer<Artifact> = this as ArtifactRenderer<Artifact>

    private fun formatType(type: KClass<out Artifact>): String = type.qualifiedName ?: type.toString()
}
