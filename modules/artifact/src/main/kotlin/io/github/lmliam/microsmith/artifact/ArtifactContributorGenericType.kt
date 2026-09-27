package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.resolve.ResolvedModel
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

internal fun ArtifactContributor<*>.findGenericResolvedType(): KClass<out ResolvedModel> {
    val contributorClass = this::class
    val contributorSupertype = contributorClass.java.findArtifactContributorSupertype()
        ?: error(
            "Unable to determine ArtifactContributor type for " +
                "${contributorClass.qualifiedName ?: contributorClass.toString()}.",
        )

    return contributorSupertype.requireResolvedModelTypeArgument()
}

private fun Class<*>.findArtifactContributorSupertype(): ParameterizedType? =
    genericInterfaces.firstNotNullOfOrNull { it.findArtifactContributorSupertype() }
        ?: genericSuperclass?.findArtifactContributorSupertype()

private fun Type.findArtifactContributorSupertype(): ParameterizedType? = when (this) {
    is ParameterizedType -> when (val rawClass = rawType) {
        ArtifactContributor::class.java -> this
        is Class<*> -> rawClass.findArtifactContributorSupertype()
        else -> null
    }

    is Class<*> -> findArtifactContributorSupertype()

    else -> null
}

private fun ParameterizedType.requireResolvedModelTypeArgument(): KClass<out ResolvedModel> {
    val modelClass = requireNotNull(actualTypeArguments.singleOrNull() as? Class<*>) {
        "ArtifactContributor registrations must declare a concrete resolved model type"
    }

    require(ResolvedModel::class.java.isAssignableFrom(modelClass)) {
        "ArtifactContributor resolved model type must implement ResolvedModel: " +
            (modelClass.canonicalName ?: modelClass.toString())
    }

    return modelClass.asSubclass(ResolvedModel::class.java).kotlin
}
