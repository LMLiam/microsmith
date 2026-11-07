package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

import arrow.core.Either
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

object ProtobufNameValidation {
    fun normalizeQualifiedName(value: String): Either<ProtobufResolutionIssue.InvalidSchemaName, String> {
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

        if (segments.any { !isIdentifier(it) }) {
            return invalid(value, ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT)
        }

        return Either.Right(segments.joinToString("."))
    }

    fun requireIdentifier(value: String, label: String) {
        require(isIdentifier(value)) { "$label is not a valid protobuf identifier: '$value'" }
    }

    fun isIdentifier(value: String): Boolean =
        value.isNotBlank() && value == value.trim() && PROTO_IDENTIFIER.matches(value)

    private fun invalid(
        value: String,
        reason: ProtobufResolutionIssue.InvalidSchemaName.Reason,
    ): Either<ProtobufResolutionIssue.InvalidSchemaName, String> =
        Either.Left(ProtobufResolutionIssue.InvalidSchemaName(value, reason))

    private val PROTO_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
