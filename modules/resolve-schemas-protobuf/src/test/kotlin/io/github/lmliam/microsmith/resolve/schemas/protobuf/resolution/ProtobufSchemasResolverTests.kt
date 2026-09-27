package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution
import io.github.lmliam.microsmith.dsl.microsmith
import io.github.lmliam.microsmith.dsl.require
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
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
                        message("User") {
                            ref("status", "Status")
                        }

                        enum("Status") {
                            +"ACTIVE"
                        }
                    }
                }
            }.require<SchemasExtension>()

            resolver.resolve(schemas)
                .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()
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

                            oneof("contact") {
                                ref("email", "MissingContact")
                            }
                        }
                    }
                }
            }.require<SchemasExtension>()

            val failure = resolver.resolve(schemas)
                .shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly listOf(
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

        "resolves local relative and qualified reference identities" {
            val schemas =
                microsmith {
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
                }.require<SchemasExtension>()

            val success =
                resolver
                    .resolve(schemas)
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufSchemaModel>>()

            val source =
                success.model.schemas
                    .single {
                        it.identity
                            .fullyQualifiedName ==
                            "root.child.Source"
                    }

            source.dependencies
                .map {
                    it.fullyQualifiedName
                } shouldContainExactly listOf("root.Root", "root.child.Peer")

            val message =
                source.declaration.shouldBeTypeOf<ResolvedProtobufMessage>()

            message.fields
                .filterIsInstance<ResolvedProtobufField.Reference>()
                .associate {
                    it.name to
                        it.reference
                            .target
                            .fullyQualifiedName
                } shouldBe
                mapOf(
                    "local" to
                        "root.child.Peer",
                    "relative" to
                        "root.Root",
                    "qualified" to
                        "root.Root",
                )
        }

        "returns a typed issue when schema and declaration names disagree" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "pkg.Contact",
                            schema = Message(
                                name = "Profile",
                            ),
                        ),
                    ),
                )

            val failure =
                resolver
                    .resolve(schemas)
                    .shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues
                .toList() shouldContainExactly
                listOf(
                    ProtobufResolutionIssue.SchemaDeclarationNameMismatch(
                        schemaName =
                        "pkg.Contact",
                        declarationName =
                        "Profile",
                    ),
                )
        }

        "accumulates independent protobuf declaration semantic issues" {
            val schemas = SchemasExtension(
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
                    ),
                ),
            )

            val failure = resolver
                .resolve(schemas)
                .shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly listOf(
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
    })
