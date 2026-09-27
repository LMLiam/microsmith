package io.github.lmliam.microsmith.resolve.diagnostics

import io.github.lmliam.microsmith.resolve.ResolutionIssue
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

internal fun ResolutionIssueDiagnosticMapper<*>.findGenericIssueType(): KClass<out ResolutionIssue> {
    val mapperClass = this::class
    val mapperSupertype = mapperClass.java.findResolutionIssueDiagnosticMapperSupertype()
        ?: error(
            "Unable to determine ResolutionIssueDiagnosticMapper type for " +
                "${mapperClass.displayName()}",
        )

    return mapperSupertype.requireResolutionIssueTypeArgument()
}

private fun Class<*>.findResolutionIssueDiagnosticMapperSupertype(): ParameterizedType? = genericInterfaces
    .firstNotNullOfOrNull { it.findResolutionIssueDiagnosticMapperSupertype() }
    ?: genericSuperclass?.findResolutionIssueDiagnosticMapperSupertype()

private fun Type.findResolutionIssueDiagnosticMapperSupertype(): ParameterizedType? = when (this) {
    is ParameterizedType -> when (val rawClass = rawType) {
        ResolutionIssueDiagnosticMapper::class.java -> this
        is Class<*> -> rawClass.findResolutionIssueDiagnosticMapperSupertype()
        else -> null
    }

    is Class<*> -> findResolutionIssueDiagnosticMapperSupertype()

    else -> null
}

private fun ParameterizedType.requireResolutionIssueTypeArgument(): KClass<out ResolutionIssue> {
    val issueClass = requireNotNull(actualTypeArguments.singleOrNull() as? Class<*>) {
        "ResolutionIssueDiagnosticMapper registrations must declare a concrete resolution issue type"
    }

    require(ResolutionIssue::class.java.isAssignableFrom(issueClass)) {
        "ResolutionIssueDiagnosticMapper issue type must implement ResolutionIssue: " +
            (issueClass.canonicalName ?: issueClass)
    }

    return issueClass
        .asSubclass(ResolutionIssue::class.java)
        .kotlin
}

private fun KClass<*>.displayName(): String = qualifiedName ?: toString()
