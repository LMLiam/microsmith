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

        "maps an invalid reference name with its location" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            mapper.map(
                ProtobufResolutionIssue.InvalidReferenceName(
                    schemaName = "User",
                    location = ProtobufResolutionIssue.ReferenceLocation.Field("owner"),
                    targetName = "pkg..Owner",
                    reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT,
                )
            ) shouldBe
                ResolutionDiagnostic(
                    code = "protobuf.invalid-reference-name",
                    message =
                        "Protobuf schema 'User' field 'owner' references invalid type name 'pkg..Owner': " +
                            "it contains an empty segment.",
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

        "maps a symbol name collision with each declaration" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            mapper.map(
                ProtobufResolutionIssue.SymbolNameCollision(
                    scope = "pkg",
                    name = "UNSPECIFIED",
                    declarations =
                        listOf(
                            ProtobufResolutionIssue.SymbolDeclaration.EnumValue("pkg.First", "UNSPECIFIED"),
                            ProtobufResolutionIssue.SymbolDeclaration.Type("pkg.UNSPECIFIED"),
                        ),
                )
            ) shouldBe
                ResolutionDiagnostic(
                    code = "protobuf.symbol-name-collision",
                    message =
                        "Protobuf scope 'pkg' declares 'UNSPECIFIED' more than once: " +
                            "enum value 'UNSPECIFIED' of 'pkg.First', type 'pkg.UNSPECIFIED'.",
                )
        }

        "maps a symbol name collision in the global scope" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            mapper.map(
                ProtobufResolutionIssue.SymbolNameCollision(
                    scope = "",
                    name = "x",
                    declarations =
                        listOf(
                            ProtobufResolutionIssue.SymbolDeclaration.Package("x"),
                            ProtobufResolutionIssue.SymbolDeclaration.Type("x"),
                        ),
                )
            ) shouldBe
                ResolutionDiagnostic(
                    code = "protobuf.symbol-name-collision",
                    message = "The global protobuf scope declares 'x' more than once: package 'x', type 'x'.",
                )
        }

        "maps a circular schema import with its path" {
            val mapper = ProtobufResolutionDiagnosticMapper()

            mapper.map(ProtobufResolutionIssue.CircularSchemaImports(listOf("pkg.A", "pkg.B", "pkg.C"))) shouldBe
                ResolutionDiagnostic(
                    code = "protobuf.circular-schema-imports",
                    message = "Protobuf schemas import each other in a cycle: pkg.A -> pkg.B -> pkg.C -> pkg.A.",
                )
        }
    })
