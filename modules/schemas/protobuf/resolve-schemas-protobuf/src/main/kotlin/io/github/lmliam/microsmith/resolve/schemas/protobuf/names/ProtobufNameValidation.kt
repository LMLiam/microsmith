package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

object ProtobufNameValidation {
    fun parseQualifiedName(value: String): Either<ProtobufResolutionIssue.InvalidSchemaName, List<ProtoIdentifier>> {
        if (value.isBlank()) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK)
        }

        if (value.startsWith(".")) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT)
        }

        if (value.endsWith(".")) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT)
        }

        if (value.any(Char::isWhitespace)) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE)
        }

        val segments = value.split('.')

        if (segments.any(String::isBlank)) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT)
        }

        return Either.Right(
            segments.map { segment ->
                ProtoIdentifier.parse(segment).getOrElse {
                    return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT)
                }
            }
        )
    }

    private fun invalid(
        value: String,
        reason: ProtobufResolutionIssue.InvalidSchemaName.Reason,
    ): Either<ProtobufResolutionIssue.InvalidSchemaName, Nothing> =
        Either.Left(ProtobufResolutionIssue.InvalidSchemaName(value, reason))
}
