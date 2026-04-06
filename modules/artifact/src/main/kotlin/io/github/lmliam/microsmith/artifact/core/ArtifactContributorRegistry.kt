package io.github.lmliam.microsmith.artifact.core

import io.github.lmliam.microsmith.resolve.core.ResolvedModel
import java.util.ServiceLoader
import kotlin.reflect.KClass

internal class ArtifactContributorRegistry(contributors: List<ArtifactContributor<*>> = loadArtifactContributors()) {
    private val contributorsByResolvedType = indexContributors(contributors)

    fun resolve(model: ResolvedModel): List<ArtifactContributor<ResolvedModel>> =
        contributorsByResolvedType[model::class]
            ?.map { it.cast() }
            .orEmpty()

    private fun indexContributors(
        contributors: List<ArtifactContributor<*>>,
    ): Map<KClass<out ResolvedModel>, List<ArtifactContributor<*>>> {
        val byResolvedType = contributors.groupBy(ArtifactContributor<*>::resolvedType)
        byResolvedType.forEach { (type, registrations) ->
            val duplicateImplementations =
                registrations
                    .groupBy { it::class }
                    .filterValues { it.size > 1 }
                    .keys
                    .map { it.qualifiedName ?: it.toString() }
                    .sorted()
            require(duplicateImplementations.isEmpty()) {
                val duplicates = duplicateImplementations.joinToString(", ")
                "Duplicate artifact contributors registered for resolved type " +
                    "${formatType(type)}: $duplicates"
            }
        }

        return byResolvedType.mapValues { (_, registrations) ->
            registrations.sortedBy { it::class.qualifiedName ?: it::class.toString() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArtifactContributor<*>.cast(): ArtifactContributor<ResolvedModel> =
        this as ArtifactContributor<ResolvedModel>

    private fun formatType(type: KClass<out ResolvedModel>): String = type.qualifiedName ?: type.toString()
}

private fun loadArtifactContributors(): List<ArtifactContributor<*>> =
    ServiceLoader.load(ArtifactContributor::class.java).iterator().asSequence().toList()
