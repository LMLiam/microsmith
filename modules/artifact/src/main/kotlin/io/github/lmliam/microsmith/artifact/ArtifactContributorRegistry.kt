package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.resolve.ResolvedModel
import kotlin.reflect.KClass

internal class ArtifactContributorRegistry(contributors: List<ArtifactContributor<*>>) {
    private val contributorsByResolvedType: Map<KClass<out ResolvedModel>, List<ArtifactContributor<*>>> =
        contributors
            .onEach(::validateResolvedTypeDeclaration)
            .groupBy { it.resolvedType }
            .mapValues { (resolvedType, registrations) ->
                requireDistinctImplementations(resolvedType, registrations)
                registrations.sortedBy { it::class.displayName() }
            }

    fun resolve(model: ResolvedModel): List<ArtifactContributor<ResolvedModel>> {
        val contributors = contributorsByResolvedType[model::class].orEmpty()

        @Suppress("UNCHECKED_CAST")
        return contributors as List<ArtifactContributor<ResolvedModel>>
    }

    private fun validateResolvedTypeDeclaration(contributor: ArtifactContributor<*>) {
        val declaredType = contributor.resolvedType
        val genericType = contributor.findGenericResolvedType()

        require(genericType == declaredType) {
            "${contributor::class.displayName()} declares resolvedType ${declaredType.displayName()}, " +
                "but implements ArtifactContributor<${genericType.displayName()}>. " +
                "Ensure resolvedType matches the ArtifactContributor<R> generic type."
        }
    }

    private fun requireDistinctImplementations(
        resolvedType: KClass<out ResolvedModel>,
        contributors: List<ArtifactContributor<*>>,
    ) {
        val duplicateImplementations = contributors
            .groupingBy { it::class }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateImplementations.isEmpty()) {
            val names = duplicateImplementations
                .map { it.displayName() }
                .sorted()
                .joinToString(", ")

            "Duplicate artifact contributors registered for resolved type ${resolvedType.displayName()}: $names"
        }
    }
}

private fun KClass<*>.displayName(): String = qualifiedName ?: toString()
