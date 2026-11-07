package io.github.lmliam.microsmith.compile

import io.github.lmliam.microsmith.artifact.Artifact
import kotlin.reflect.KClass

internal class ArtifactCompilerRegistry(compilers: List<ArtifactCompiler<*>>) {
    private val compilersByType: Map<KClass<out Artifact>, ArtifactCompiler<*>>

    init {
        val duplicates = compilers.groupBy { it.artifactType }.filterValues { it.size > 1 }.keys

        require(duplicates.isEmpty()) {
            val types = duplicates.map { it.displayName }.sorted().joinToString(", ")
            "Duplicate artifact compilers registered for artifact type(s): $types"
        }

        compilersByType = compilers.associateBy { it.artifactType }
    }

    @Suppress("UNCHECKED_CAST")
    fun <A : Artifact> resolveOrNull(artifact: A): ArtifactCompiler<A>? =
        compilersByType[artifact.id.artifactType] as ArtifactCompiler<A>?
}

internal val KClass<*>.displayName: String
    get() = qualifiedName ?: simpleName ?: toString()
