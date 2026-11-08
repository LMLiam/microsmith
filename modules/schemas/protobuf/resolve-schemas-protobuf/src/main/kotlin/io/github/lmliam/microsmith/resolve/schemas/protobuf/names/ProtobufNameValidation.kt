package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

object ProtobufNameValidation {
    fun parseQualifiedName(value: String): Either<ProtobufResolutionIssue.InvalidSchemaName, List<ProtoIdentifier>> {
        val normalized = value.trim()

        if (normalized.isBlank()) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK)
        }

        if (normalized.startsWith(".")) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT)
        }

        if (normalized.endsWith(".")) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT)
        }

        if (normalized.any(Char::isWhitespace)) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE)
        }

        val segments = normalized.split('.')

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

    fun requireIdentifier(value: String, label: String) {
        require(ProtoIdentifier.parse(value).isRight()) { "$label is not a valid protobuf identifier: '$value'" }
    }

    private fun invalid(
        value: String,
        reason: ProtobufResolutionIssue.InvalidSchemaName.Reason,
    ): Either<ProtobufResolutionIssue.InvalidSchemaName, Nothing> =
        Either.Left(ProtobufResolutionIssue.InvalidSchemaName(value, reason))
}
