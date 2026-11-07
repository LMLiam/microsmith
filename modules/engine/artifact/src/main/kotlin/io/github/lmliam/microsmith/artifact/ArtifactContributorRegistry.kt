package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.resolve.ResolvedModel
import kotlin.reflect.KClass

internal class ArtifactContributorRegistry(contributors: List<ArtifactContributor<*>>) {
    private val contributorsByResolvedType: Map<KClass<out ResolvedModel>, List<ArtifactContributor<*>>>

    init {
        contributors.forEach(::validateResolvedTypeDeclaration)

        contributorsByResolvedType =
            contributors
                .groupBy { it.resolvedType }
                .mapValues { (resolvedType, registrations) ->
                    requireDistinctImplementations(resolvedType, registrations)
                    registrations.sortedBy { it::class.displayName }
                }
    }

    @Suppress("UNCHECKED_CAST")
    fun <M : ResolvedModel> resolve(model: M): List<ArtifactContributor<M>> {
        val contributors = contributorsByResolvedType[model::class].orEmpty()
        return contributors as List<ArtifactContributor<M>>
    }

    private fun validateResolvedTypeDeclaration(contributor: ArtifactContributor<*>) {
        val declaredType = contributor.resolvedType
        val genericType = contributor.findGenericResolvedType()

        require(genericType == declaredType) {
            "'${contributor::class.displayName}' declares resolvedType '${declaredType.displayName}', " +
                "but implements ArtifactContributor<${genericType.displayName}>. " +
                "Ensure resolvedType matches the ArtifactContributor generic type argument."
        }
    }

    private fun requireDistinctImplementations(
        resolvedType: KClass<out ResolvedModel>,
        contributors: List<ArtifactContributor<*>>,
    ) {
        val duplicates = contributors.groupBy { it::class }.filterValues { it.size > 1 }.keys

        require(duplicates.isEmpty()) {
            val names = duplicates.map { it.displayName }.sorted().joinToString(", ")
            "Duplicate artifact contributors registered for resolved type '${resolvedType.displayName}': $names"
        }
    }
}

private val KClass<*>.displayName: String
    get() = qualifiedName ?: simpleName ?: toString()
