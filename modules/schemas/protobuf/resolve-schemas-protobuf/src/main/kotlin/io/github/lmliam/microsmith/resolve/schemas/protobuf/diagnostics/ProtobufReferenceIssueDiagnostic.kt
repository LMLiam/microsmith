package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun ProtobufResolutionIssue.ReferenceIssue.toDiagnostic(): ResolutionDiagnostic =
    when (this) {
        is ProtobufResolutionIssue.UnresolvedReference ->
            protobufDiagnostic(
                code = "protobuf.unresolved-reference",
                message =
                    "Protobuf schema '$schemaName' ${location.displayName()} " +
                        "references unknown type '$targetName'.",
            )

        is ProtobufResolutionIssue.InvalidReferenceName ->
            protobufDiagnostic(
                code = "protobuf.invalid-reference-name",
                message =
                    "Protobuf schema '$schemaName' ${location.displayName()} " +
                        "references invalid type name '$targetName': it ${reason.displayName()}.",
            )

        is ProtobufResolutionIssue.CircularSchemaImports ->
            protobufDiagnostic(
                code = "protobuf.circular-schema-imports",
                message =
                    "Protobuf schemas import each other in a cycle: " +
                        (schemas + schemas.first()).joinToString(" -> ") +
                        ".",
            )
    }
