package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun protobufDiagnostic(code: String, message: String): ResolutionDiagnostic =
    ResolutionDiagnostic(
        code = code,
        message = message,
    )

internal fun ProtobufResolutionIssue.InvalidSchemaName.Reason.displayName(): String =
    when (this) {
        ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK -> "is blank"

        ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT -> "starts with a leading dot"

        ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT -> "ends with a trailing dot"

        ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE -> "contains whitespace"

        ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT -> "contains an empty segment"

        ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT ->
            "contains an invalid identifier segment"
    }

internal fun ProtobufResolutionIssue.ReferenceLocation.displayName(): String =
    when (this) {
        is ProtobufResolutionIssue.ReferenceLocation.Field -> "field '$fieldName'"

        is ProtobufResolutionIssue.ReferenceLocation.MapValue -> "map field '$fieldName' value"

        is ProtobufResolutionIssue.ReferenceLocation.OneofField -> "oneof '$oneofName' field '$fieldName'"
    }

internal fun ProtobufResolutionIssue.IdentifierLocation.displayName(): String =
    when (this) {
        ProtobufResolutionIssue.IdentifierLocation.Declaration -> "declaration"

        is ProtobufResolutionIssue.IdentifierLocation.Field -> "field '$fieldName'"

        is ProtobufResolutionIssue.IdentifierLocation.Oneof -> "oneof '$oneofName'"

        is ProtobufResolutionIssue.IdentifierLocation.OneofField -> "oneof '$oneofName' field '$fieldName'"

        is ProtobufResolutionIssue.IdentifierLocation.EnumValue -> "enum value '$valueName'"

        is ProtobufResolutionIssue.IdentifierLocation.Reservation -> "reserved name '$name'"
    }

internal fun ProtobufResolutionIssue.FieldLocation.displayName(): String =
    when (this) {
        is ProtobufResolutionIssue.FieldLocation.Field -> "field '$fieldName'"

        is ProtobufResolutionIssue.FieldLocation.OneofField -> "oneof '$oneofName' field '$fieldName'"

        is ProtobufResolutionIssue.FieldLocation.Reservation -> "reservation '$description'"
    }

internal fun String.displayScope(): String = if (isEmpty()) "The global protobuf scope" else "Protobuf scope '$this'"

internal fun ProtobufResolutionIssue.SymbolDeclaration.displayName(): String =
    when (this) {
        is ProtobufResolutionIssue.SymbolDeclaration.Package -> "package '$packageName'"

        is ProtobufResolutionIssue.SymbolDeclaration.Type -> "type '$schemaName'"

        is ProtobufResolutionIssue.SymbolDeclaration.EnumValue -> "enum value '$valueName' of '$schemaName'"

        is ProtobufResolutionIssue.SymbolDeclaration.Field -> "field '$fieldName' of '$schemaName'"

        is ProtobufResolutionIssue.SymbolDeclaration.Oneof -> "oneof '$oneofName' of '$schemaName'"

        is ProtobufResolutionIssue.SymbolDeclaration.MapEntry -> "map entry of field '$fieldName' of '$schemaName'"
    }
