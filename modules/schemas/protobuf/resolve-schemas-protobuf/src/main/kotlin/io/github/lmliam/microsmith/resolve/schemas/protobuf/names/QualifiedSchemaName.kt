package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

@ConsistentCopyVisibility
data class QualifiedSchemaName
private constructor(
    val fullyQualifiedName: String,
    val packageName: String?,
    val typeName: ProtoIdentifier,
) {
    fun resolveReference(reference: Reference): Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName> =
        parse(reference.resolveSegments(packageName?.split('.').orEmpty()).joinToString("."))

    internal fun withTypeName(typeName: ProtoIdentifier): QualifiedSchemaName =
        QualifiedSchemaName(
            fullyQualifiedName = packageName?.let { "$it.$typeName" } ?: typeName.value,
            packageName = packageName,
            typeName = typeName,
        )

    companion object {
        fun parse(rawName: String): Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName> =
            ProtobufNameValidation.parseQualifiedName(rawName).map { segments ->
                QualifiedSchemaName(
                    fullyQualifiedName = segments.joinToString("."),
                    packageName = segments.dropLast(1).joinToString(".").ifEmpty { null },
                    typeName = segments.last(),
                )
            }
    }
}
