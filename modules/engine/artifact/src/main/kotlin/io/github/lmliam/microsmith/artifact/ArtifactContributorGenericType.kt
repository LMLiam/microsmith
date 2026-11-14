package io.github.lmliam.microsmith.artifact

import io.github.lmliam.microsmith.resolve.ResolvedModel
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass

internal fun ArtifactContributor<*>.findGenericResolvedType(): KClass<out ResolvedModel> {
    val contributorClass = this::class.java
    val targetInterface = ArtifactContributor::class.java

    val supertype =
        contributorClass.findParameterizedSupertype(targetInterface)
            ?: error("Unable to determine ArtifactContributor type argument for '${this::class.displayName}'.")

    return supertype.extractModelClass()
}

private fun Type.findParameterizedSupertype(targetRawClass: Class<*>): ParameterizedType? =
    when (this) {
        is ParameterizedType if rawType == targetRawClass -> this

        is ParameterizedType -> (rawType as? Class<*>)?.findParameterizedSupertype(targetRawClass)

        is Class<*> -> {
            genericInterfaces.firstNotNullOfOrNull { it.findParameterizedSupertype(targetRawClass) }
                ?: genericSuperclass?.findParameterizedSupertype(targetRawClass)
        }

        else -> {
            null
        }
    }

private fun ParameterizedType.extractModelClass(): KClass<out ResolvedModel> {
    val typeArg =
        actualTypeArguments.singleOrNull()
            ?: error("ArtifactContributor interface must declare exactly one generic type argument.")

    val modelClass =
        typeArg.toRawClass()
            ?: error("ArtifactContributor type argument '$typeArg' cannot be resolved to a concrete class.")

    require(ResolvedModel::class.java.isAssignableFrom(modelClass)) {
        "ArtifactContributor model type '${modelClass.displayName}' must implement ResolvedModel."
    }

    @Suppress("UNCHECKED_CAST")
    return (modelClass as Class<out ResolvedModel>).kotlin
}

private fun Type.toRawClass(): Class<*>? =
    when (this) {
        is Class<*> -> this
        is ParameterizedType -> rawType as? Class<*>
        is WildcardType -> upperBounds.firstOrNull()?.toRawClass()
        else -> null
    }

private val KClass<*>.displayName: String
    get() = qualifiedName ?: simpleName ?: toString()

private val Class<*>.displayName: String
    get() = canonicalName ?: name
