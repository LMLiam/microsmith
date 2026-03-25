package io.github.lmliam.microsmith.compile.core

import io.github.lmliam.microsmith.artifact.core.Artifact
import java.util.ServiceLoader
import kotlin.reflect.KClass

internal class ArtifactCompilerRegistry(
    compilers: List<ArtifactCompiler<*>> = loadArtifactCompilers(),
) {
    private val compilersByType: Map<KClass<out Artifact>, ArtifactCompiler<*>> = indexCompilers(compilers)

    fun resolveOrNull(artifact: Artifact): ArtifactCompiler<Artifact>? =
        compilersByType[artifact.id.artifactType]?.cast()

    private fun indexCompilers(compilers: List<ArtifactCompiler<*>>): Map<KClass<out Artifact>, ArtifactCompiler<*>> {
        val duplicates = compilers.groupBy(ArtifactCompiler<*>::artifactType).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate artifact compilers registered for artifact types: $types"
        }

        return compilers.associateBy(ArtifactCompiler<*>::artifactType)
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactCompiler<*>.cast(): ArtifactCompiler<Artifact> = this as ArtifactCompiler<Artifact>

    private fun formatType(type: KClass<out Artifact>): String = type.qualifiedName ?: type.toString()
}

private fun loadArtifactCompilers(): List<ArtifactCompiler<*>> = ServiceLoader.load(ArtifactCompiler::class.java)
    .iterator()
    .asSequence()
    .toList()
