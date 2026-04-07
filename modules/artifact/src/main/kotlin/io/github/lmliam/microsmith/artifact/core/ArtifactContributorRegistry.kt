package io.github.lmliam.microsmith.artifact.core

import io.github.lmliam.microsmith.resolve.core.ResolvedModel
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
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
        contributors.forEach(::validateResolvedTypeDeclaration)
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

    private fun validateResolvedTypeDeclaration(contributor: ArtifactContributor<*>) {
        val contributorName = contributor::class.qualifiedName ?: contributor::class.toString()
        val declaredType = contributor.resolvedType
        val genericType = contributor.findGenericResolvedType()
        require(genericType == declaredType) {
            "$contributorName declares resolvedType ${formatType(declaredType)}, but implements " +
                "ArtifactContributor<${formatType(genericType)}>."
        }
    }

    private fun ArtifactContributor<*>.findGenericResolvedType(): KClass<out ResolvedModel> {
        val contributorSupertype =
            this::class.java
                .findArtifactContributorType()
                ?: error(
                    "Unable to determine ArtifactContributor type for " +
                        (this::class.qualifiedName ?: this::class.toString()) +
                        ".",
                )
        return contributorSupertype.resolvedTypeArgument()
    }

    private fun ParameterizedType.resolvedTypeArgument(): KClass<out ResolvedModel> {
        val resolvedType = actualTypeArguments.singleOrNull() as? Class<*>
        requireNotNull(resolvedType) {
            "ArtifactContributor registrations must declare a concrete resolved model type."
        }
        require(ResolvedModel::class.java.isAssignableFrom(resolvedType)) {
            "ArtifactContributor resolved model type must implement ResolvedModel: " +
                (resolvedType.canonicalName ?: resolvedType.toString())
        }
        @Suppress("UNCHECKED_CAST")
        return resolvedType.kotlin as KClass<out ResolvedModel>
    }

    private fun formatType(type: KClass<out ResolvedModel>): String = type.qualifiedName ?: type.toString()
}

private fun Class<*>.findArtifactContributorType(): ParameterizedType? {
    val interfaceContributorType =
        genericInterfaces
            .asSequence()
            .mapNotNull(Type::findArtifactContributorType)
            .firstOrNull()
    return interfaceContributorType ?: genericSuperclass?.findArtifactContributorType()
}

private fun Type.findArtifactContributorType(): ParameterizedType? = when (this) {
    is ParameterizedType ->
        when (val rawType = rawType) {
            ArtifactContributor::class.java -> this
            is Class<*> -> rawType.findArtifactContributorType()
            else -> null
        }

    is Class<*> -> findArtifactContributorType()

    else -> null
}

private fun loadArtifactContributors(): List<ArtifactContributor<*>> =
    ServiceLoader.load(ArtifactContributor::class.java).iterator().asSequence().toList()
