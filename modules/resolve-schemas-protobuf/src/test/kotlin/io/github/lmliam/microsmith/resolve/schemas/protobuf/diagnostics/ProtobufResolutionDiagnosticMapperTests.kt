package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class ProtobufResolutionDiagnosticMapperTests :
    StringSpec({
        "maps unresolved reference locations" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            listOf(
                ProtobufResolutionIssue
                    .UnresolvedReference(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.Field("manager"),
                        targetName = "MissingUser",
                    ),
                ProtobufResolutionIssue
                    .UnresolvedReference(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.MapValue("labels"),
                        targetName = "MissingLabel",
                    ),
                ProtobufResolutionIssue
                    .UnresolvedReference(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.OneofField(
                            oneofName = "contact",
                            fieldName = "email",
                        ),
                        targetName = "MissingContact",
                    ),
            ).map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "protobuf.unresolved-reference",
                        message = "Protobuf schema 'User' field " +
                            "'manager' references unknown " +
                            "type 'MissingUser'.",
                    ),
                    ResolutionDiagnostic(
                        code = "protobuf.unresolved-reference",
                        message = "Protobuf schema 'User' map field " +
                            "'labels' value references unknown " +
                            "type 'MissingLabel'.",
                    ),
                    ResolutionDiagnostic(
                        code = "protobuf.unresolved-reference",
                        message = "Protobuf schema 'User' oneof " +
                            "'contact' field 'email' references " +
                            "unknown type 'MissingContact'.",
                    ),
                )
        }
    })
