package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import arrow.core.getOrElse
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
            parseQualifiedName(rawName).map { segments ->
                QualifiedSchemaName(
                    fullyQualifiedName = segments.joinToString("."),
                    packageName = segments.dropLast(1).joinToString(".").ifEmpty { null },
                    typeName = segments.last(),
                )
            }
    }
}

private fun parseQualifiedName(
    value: String,
): Either<ProtobufResolutionIssue.InvalidSchemaName, List<ProtoIdentifier>> {
    if (value.isBlank()) {
        return invalidSchemaName(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK)
    }

    if (value.startsWith(".")) {
        return invalidSchemaName(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT)
    }

    if (value.endsWith(".")) {
        return invalidSchemaName(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT)
    }

    if (value.any(Char::isWhitespace)) {
        return invalidSchemaName(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE)
    }

    val segments = value.split('.')

    if (segments.any(String::isBlank)) {
        return invalidSchemaName(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT)
    }

    return Either.Right(
        segments.map { segment ->
            ProtoIdentifier.parse(segment).getOrElse {
                return invalidSchemaName(
                    value,
                    ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT,
                )
            }
        },
    )
}

private fun invalidSchemaName(
    value: String,
    reason: ProtobufResolutionIssue.InvalidSchemaName.Reason,
): Either<ProtobufResolutionIssue.InvalidSchemaName, Nothing> =
    Either.Left(ProtobufResolutionIssue.InvalidSchemaName(value, reason))
