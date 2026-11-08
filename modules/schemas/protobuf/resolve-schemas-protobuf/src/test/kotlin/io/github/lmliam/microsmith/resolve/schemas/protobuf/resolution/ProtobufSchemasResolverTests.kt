package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.microsmith
import io.github.lmliam.microsmith.dsl.require
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.dsl.schemas.schemas
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufSchemasResolverTests :
    StringSpec({
        val resolver = ProtobufSchemasResolver()

        "resolves schemas whose references exist" {
            val schemas = microsmith {
                schemas {
                    protobuf {
                        message("User") { ref("status", "Status") }

                        enum("Status") { +"ACTIVE" }
                    }
                }
            }
                .require<SchemasExtension>()

            resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
        }

        "accumulates unresolved reference locations" {
            val schemas = microsmith {
                schemas {
                    protobuf {
                        message("User") {
                            ref("manager", "MissingManager")

                            map("labels") {
                                key(string)
                                value("MissingLabel")
                            }

                            oneof("contact") { ref("email", "MissingContact") }
                        }
                    }
                }
            }
                .require<SchemasExtension>()

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.Field("manager"),
                        "MissingManager",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.MapValue("labels"),
                        "MissingLabel",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.OneofField("contact", "email"),
                        "MissingContact",
                    ),
                )
        }

        "reports every unresolved reference across fields, map values and each oneof" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "User",
                            schema =
                                Message(
                                    name = "User",
                                    fields =
                                        listOf(
                                            ReferenceField("manager", 1, Reference.Local("MissingManager")),
                                            ReferenceField("peer", 2, Reference.Local("MissingPeer")),
                                            MapField(
                                                "labels",
                                                3,
                                                MapType(PrimitiveType.STRING, Reference.Local("MissingLabel")),
                                            ),
                                        ),
                                    oneofs =
                                        listOf(
                                            Oneof(
                                                "contact",
                                                listOf(
                                                    OneofField("email", 4, Reference.Local("MissingEmail")),
                                                    OneofField("phone", 5, Reference.Local("MissingPhone")),
                                                ),
                                            ),
                                            Oneof(
                                                "account",
                                                listOf(OneofField("owner", 6, Reference.Local("MissingOwner"))),
                                            ),
                                        ),
                                ),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.Field("manager"),
                        "MissingManager",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.Field("peer"),
                        "MissingPeer",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.MapValue("labels"),
                        "MissingLabel",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.OneofField("contact", "email"),
                        "MissingEmail",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.OneofField("contact", "phone"),
                        "MissingPhone",
                    ),
                    ProtobufResolutionIssue.UnresolvedReference(
                        "User",
                        ProtobufResolutionIssue.ReferenceLocation.OneofField("account", "owner"),
                        "MissingOwner",
                    ),
                )
        }

        "resolves local relative and qualified reference identities" {
            val schemas = microsmith {
                schemas {
                    protobuf {
                        "root" {
                            message("Root")

                            "child" {
                                message("Peer")

                                message("Source") {
                                    ref("local", "Peer")
                                    ref("relative", ".Root")
                                    ref("qualified", "root.Root")
                                }
                            }
                        }
                    }
                }
            }
                .require<SchemasExtension>()

            val success =
                resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()

            val source = success.model.schemas.single { it.identity.fullyQualifiedName == "root.child.Source" }

            source.dependencies.map { it.fullyQualifiedName } shouldContainExactly
                listOf("root.Root", "root.child.Peer")

            val message = source.declaration.shouldBeTypeOf<ResolvedProtobufMessage>()

            message.fields.filterIsInstance<ResolvedProtobufField.Reference>().associate {
                it.name.value to it.reference.target.fullyQualifiedName
            } shouldBe
                mapOf(
                    "local" to "root.child.Peer",
                    "relative" to "root.Root",
                    "qualified" to "root.Root",
                )
        }

        "ignores declarations the resolver does not own" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(name = "User", schema = Message(name = "User")),
                        ProtobufSchema(name = "Broken", schema = UnknownDeclaration(name = "Broken")),
                    )
                )

            val success =
                resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()

            success.model.schemas.single().declaration.shouldBeTypeOf<ResolvedProtobufMessage>()
        }

        "returns a typed issue when schema and declaration names disagree" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "pkg.Contact",
                            schema = Message(name = "Profile"),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.SchemaDeclarationNameMismatch(
                        schemaName = "pkg.Contact",
                        declarationName = "Profile",
                    )
                )
        }

        "reports an invalid declaration name before a declaration name mismatch" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "pkg.Contact",
                            schema = Message(name = "user-name"),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidIdentifier(
                        schemaName = "pkg.Contact",
                        location = ProtobufResolutionIssue.IdentifierLocation.Declaration,
                        value = "user-name",
                    ),
                    ProtobufResolutionIssue.SchemaDeclarationNameMismatch(
                        schemaName = "pkg.Contact",
                        declarationName = "user-name",
                    ),
                )
        }

        "accumulates independent protobuf declaration semantic issues" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            "Broken",
                            Message(
                                "Broken",
                                listOf(
                                    ScalarField("duplicate", 0, PrimitiveType.STRING),
                                    ScalarField("duplicate", 0, PrimitiveType.STRING),
                                ),
                            ),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "Broken",
                        ProtobufResolutionIssue.FieldLocation.Field("duplicate"),
                        0,
                    ),
                    ProtobufResolutionIssue.InvalidFieldNumber(
                        "Broken",
                        ProtobufResolutionIssue.FieldLocation.Field("duplicate"),
                        0,
                    ),
                    ProtobufResolutionIssue.DuplicateFieldNames("Broken", listOf("duplicate")),
                    ProtobufResolutionIssue.DuplicateFieldNumbers("Broken", listOf(0)),
                )
        }

        "accumulates enum value issues before a declaration name mismatch" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "pkg.Contact",
                            schema =
                                Enum(
                                    name = "Status",
                                    values = listOf(EnumValue("ACTIVE", 1), EnumValue("ACTIVE", 1)),
                                ),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.EnumFirstValueMustBeZero("pkg.Contact", "ACTIVE", 1),
                    ProtobufResolutionIssue.DuplicateEnumValueNames("pkg.Contact", listOf("ACTIVE")),
                    ProtobufResolutionIssue.DuplicateEnumValueNumbers("pkg.Contact", listOf(1)),
                    ProtobufResolutionIssue.SchemaDeclarationNameMismatch("pkg.Contact", "Status"),
                )
        }

        "returns a typed issue when a reference name is blank" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "User",
                            schema =
                                Message(
                                    name = "User",
                                    fields = listOf(ReferenceField("owner", 1, Reference.Qualified(""))),
                                ),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidReferenceName(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.Field("owner"),
                        targetName = "",
                        reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK,
                    )
                )
        }

        "returns a typed issue at each reference location when a reference name is invalid" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "User",
                            schema =
                                Message(
                                    name = "User",
                                    fields =
                                        listOf(
                                            ReferenceField("owner", 1, Reference.Qualified("pkg..Owner")),
                                            MapField(
                                                "labels",
                                                2,
                                                MapType(PrimitiveType.STRING, Reference.Qualified("Label.")),
                                            ),
                                        ),
                                    oneofs =
                                        listOf(
                                            Oneof("contact", listOf(OneofField("email", 3, Reference.Qualified("a b"))))
                                        ),
                                ),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidReferenceName(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.Field("owner"),
                        targetName = "pkg..Owner",
                        reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT,
                    ),
                    ProtobufResolutionIssue.InvalidReferenceName(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.MapValue("labels"),
                        targetName = "Label.",
                        reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT,
                    ),
                    ProtobufResolutionIssue.InvalidReferenceName(
                        schemaName = "User",
                        location = ProtobufResolutionIssue.ReferenceLocation.OneofField("contact", "email"),
                        targetName = "a b",
                        reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE,
                    ),
                )
        }

        "returns a typed issue when a schema name starts with a dot" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = ".User",
                            schema = Message(name = "User"),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidSchemaName(
                        schemaName = ".User",
                        reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT,
                    )
                )
        }

        "returns a typed issue when a schema name has an invalid identifier segment" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "pkg.1User",
                            schema = Message(name = "User"),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.InvalidSchemaName(
                        schemaName = "pkg.1User",
                        reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT,
                    )
                )
        }
    })

private class UnknownDeclaration(override val name: String) : Type
