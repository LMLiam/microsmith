package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import arrow.core.getOrElse
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

@ConsistentCopyVisibility
public data class QualifiedSchemaName
private constructor(
    public val fullyQualifiedName: String,
    public val packageName: String?,
    public val typeName: ProtoIdentifier,
) {
    public fun resolveReference(
        reference: Reference,
    ): Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName> =
        parse(reference.resolveSegments(packageName?.split('.').orEmpty()).joinToString("."))

    internal fun withTypeName(typeName: ProtoIdentifier): QualifiedSchemaName =
        QualifiedSchemaName(
            fullyQualifiedName = packageName?.let { "$it.$typeName" } ?: typeName.value,
            packageName = packageName,
            typeName = typeName,
        )

    public companion object {
        public fun parse(rawName: String): Either<ProtobufResolutionIssue.InvalidSchemaName, QualifiedSchemaName> =
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
    val segments = value.split('.')
    val reason =
        when {
            value.isBlank() -> ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK
            value.startsWith(".") -> ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT
            value.endsWith(".") -> ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT
            value.any(Char::isWhitespace) -> ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE
            segments.any(String::isBlank) -> ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT
            else -> null
        }
    if (reason != null) return invalidSchemaName(value, reason)

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
