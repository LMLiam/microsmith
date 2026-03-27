package io.github.lmliam.microsmith.gen.core

import io.github.lmliam.microsmith.artifact.core.Artifact
import java.util.ServiceLoader
import kotlin.reflect.KClass

class ArtifactRendererRegistry(
    renderers: List<ArtifactRenderer<*>> = loadArtifactRenderers(),
) {
    private val renderersByType: Map<KClass<out Artifact>, ArtifactRenderer<*>> = indexRenderers(renderers)

    fun resolve(artifact: Artifact): ArtifactRenderer<Artifact> {
        return renderersByType[artifact.id.artifactType]
            ?.cast()
            ?: error("No artifact renderer found for artifact type: ${artifact.id.artifactType}")
    }

    private fun indexRenderers(renderers: List<ArtifactRenderer<*>>): Map<KClass<out Artifact>, ArtifactRenderer<*>> {
        val duplicates = renderers.groupBy(ArtifactRenderer<*>::artifactType).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate artifact renderers registered for artifact types: $types"
        }

        return renderers.associateBy(ArtifactRenderer<*>::artifactType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactRenderer<*>.cast(): ArtifactRenderer<Artifact> = this as ArtifactRenderer<Artifact>

    private fun formatType(type: KClass<out Artifact>): String = type.qualifiedName ?: type.toString()
}

private fun loadArtifactRenderers(): List<ArtifactRenderer<*>> = ServiceLoader.load(ArtifactRenderer::class.java)
    .iterator()
    .asSequence()
    .toList()
