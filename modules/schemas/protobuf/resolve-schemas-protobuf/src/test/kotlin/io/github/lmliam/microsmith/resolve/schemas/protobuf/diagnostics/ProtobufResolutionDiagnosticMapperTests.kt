package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ProtobufResolutionDiagnosticMapperTests :
    StringSpec({
        "maps unresolved reference locations" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            listOf(
                    ProtobufResolutionIssue.UnresolvedReference(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.Field("manager"),
                        targetName = "MissingUser",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.MapValue("labels"),
                        targetName = "MissingLabel",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        schemaName = "User",
                        location =
                            ProtobufResolutionIssue.ReferenceLocation.OneofField(
                                oneofName = "contact",
                                fieldName = "email",
                            ),
                        targetName = "MissingContact",
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "protobuf.unresolved-reference",
                        message =
                            "Protobuf schema 'User' field " + "'manager' references unknown " + "type 'MissingUser'.",
                    ),
                    ResolutionDiagnostic(
                        code = "protobuf.unresolved-reference",
                        message =
                            "Protobuf schema 'User' map field " +
                                "'labels' value references unknown " +
                                "type 'MissingLabel'.",
                    ),
                    ResolutionDiagnostic(
                        code = "protobuf.unresolved-reference",
                        message =
                            "Protobuf schema 'User' oneof " +
                                "'contact' field 'email' references " +
                                "unknown type 'MissingContact'.",
                    ),
                )
        }

        "maps an invalid schema name" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            mapper.map(
                ProtobufResolutionIssue.InvalidSchemaName(
                    schemaName = "pkg.1User",
                    reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT,
                )
            ) shouldBe
                ResolutionDiagnostic(
                    code = "protobuf.invalid-schema-name",
                    message =
                        "Protobuf schema name 'pkg.1User' is invalid: it contains an " + "invalid identifier segment.",
                )
        }
    })
