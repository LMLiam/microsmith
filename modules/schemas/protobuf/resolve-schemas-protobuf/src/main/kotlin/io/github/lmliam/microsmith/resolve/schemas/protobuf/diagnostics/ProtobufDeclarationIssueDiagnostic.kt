package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun ProtobufResolutionIssue.DeclarationIssue.toDiagnostic(): ResolutionDiagnostic =
    when (this) {
        is ProtobufResolutionIssue.SchemaDeclarationNameMismatch ->
            protobufDiagnostic(
                code = "protobuf.schema-declaration-name-mismatch",
                message =
                    "Protobuf schema '$schemaName' declares '$declarationName', " +
                        "but the schema identity and declaration name must match.",
            )

        is ProtobufResolutionIssue.InvalidIdentifier ->
            protobufDiagnostic(
                code = "protobuf.invalid-identifier",
                message =
                    "Protobuf schema '$schemaName' ${location.displayName()} " + "has invalid identifier '$value'.",
            )

        is ProtobufResolutionIssue.InvalidFieldNumber ->
            protobufDiagnostic(
                code = "protobuf.invalid-field-number",
                message =
                    "Protobuf schema '$schemaName' ${location.displayName()} " + "uses invalid field number $number.",
            )

        is ProtobufResolutionIssue.DuplicateFieldNames ->
            protobufDiagnostic(
                code = "protobuf.duplicate-field-names",
                message = "Protobuf schema '$schemaName' has duplicate field names: " + names.joinToString(),
            )

        is ProtobufResolutionIssue.DuplicateFieldNumbers ->
            protobufDiagnostic(
                code = "protobuf.duplicate-field-numbers",
                message = "Protobuf schema '$schemaName' has duplicate field numbers: " + numbers.joinToString(),
            )

        is ProtobufResolutionIssue.DuplicateOneofNames ->
            protobufDiagnostic(
                code = "protobuf.duplicate-oneof-names",
                message = "Protobuf schema '$schemaName' has duplicate oneof names: " + names.joinToString(),
            )

        is ProtobufResolutionIssue.EmptyOneof ->
            protobufDiagnostic(
                code = "protobuf.empty-oneof",
                message = "Protobuf schema '$schemaName' oneof '$oneofName' has no fields.",
            )
    }
