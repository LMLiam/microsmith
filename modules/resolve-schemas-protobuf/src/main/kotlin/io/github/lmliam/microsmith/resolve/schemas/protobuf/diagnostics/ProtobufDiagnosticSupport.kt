package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun protobufDiagnostic(code: String, message: String): ResolutionDiagnostic = ResolutionDiagnostic(
    code = code,
    message = message,
)

internal fun ProtobufResolutionIssue.ReferenceLocation.displayName(): String = when (this) {
    is ProtobufResolutionIssue.ReferenceLocation.Field ->
        "field '$fieldName'"

    is ProtobufResolutionIssue.ReferenceLocation.MapValue ->
        "map field '$fieldName' value"

    is ProtobufResolutionIssue.ReferenceLocation.OneofField ->
        "oneof '$oneofName' field '$fieldName'"
}

internal fun ProtobufResolutionIssue.IdentifierLocation.displayName(): String = when (this) {
    ProtobufResolutionIssue.IdentifierLocation.Declaration ->
        "declaration"

    is ProtobufResolutionIssue.IdentifierLocation.Field ->
        "field '$fieldName'"

    is ProtobufResolutionIssue.IdentifierLocation.Oneof ->
        "oneof '$oneofName'"

    is ProtobufResolutionIssue.IdentifierLocation.OneofField ->
        "oneof '$oneofName' field '$fieldName'"

    is ProtobufResolutionIssue.IdentifierLocation.EnumValue ->
        "enum value '$valueName'"

    is ProtobufResolutionIssue.IdentifierLocation.Reservation ->
        "reserved name '$name'"
}

internal fun ProtobufResolutionIssue.FieldLocation.displayName(): String = when (this) {
    is ProtobufResolutionIssue.FieldLocation.Field ->
        "field '$fieldName'"

    is ProtobufResolutionIssue.FieldLocation.OneofField ->
        "oneof '$oneofName' field '$fieldName'"

    is ProtobufResolutionIssue.FieldLocation.Reservation ->
        "reservation '$description'"
}
