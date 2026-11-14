package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufSchemaModelTests :
    StringSpec(
        {
            fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

            fun schema(fullName: String, vararg targets: String): ResolvedProtobufSchema {
                val identity = name(fullName)
                val fields = targets.mapIndexed { index, target ->
                    ResolvedProtobufField.Reference(
                        ProtoIdentifier.parse("to$index").getOrElse { error("not an identifier") },
                        FieldNumber.parse(index + 1).getOrElse { error("not a field number") },
                        ResolvedProtobufReference(name(target), ProtobufDeclarationKind.MESSAGE),
                        Cardinality.SINGULAR,
                    )
                }
                val message =
                    ResolvedProtobufMessage.of(
                            identity.typeName,
                            fields,
                            emptyList(),
                            ResolvedProtobufReservations.NONE,
                        )
                        .getOrElse { error("invalid message: $it") }

                return ResolvedProtobufSchema.of(message, packageOf = identity)
            }

            fun enumSchema(fullName: String, vararg values: String): ResolvedProtobufSchema {
                val identity = name(fullName)
                val declaration =
                    ResolvedProtobufEnum.of(
                            identity.typeName,
                            values
                                .mapIndexed { index, value ->
                                    ResolvedProtobufEnumValue(
                                        ProtoIdentifier.parse(value).getOrElse { error("not an identifier") },
                                        EnumValueNumber(index),
                                    )
                                }
                                .toNonEmptyListOrNull() ?: error("an enum needs values"),
                            ResolvedProtobufReservations.NONE,
                        )
                        .getOrElse { error("invalid enum: $it") }

                return ResolvedProtobufSchema.of(declaration, packageOf = identity)
            }

            fun messageSchema(
                fullName: String,
                fields: List<ResolvedProtobufField> = emptyList(),
                oneofs: List<ResolvedProtobufOneof> = emptyList(),
            ): ResolvedProtobufSchema {
                val identity = name(fullName)
                val message =
                    ResolvedProtobufMessage.of(identity.typeName, fields, oneofs, ResolvedProtobufReservations.NONE)
                        .getOrElse { error("invalid message: $it") }

                return ResolvedProtobufSchema.of(message, packageOf = identity)
            }

            fun referenceTo(target: String, kind: ProtobufDeclarationKind = ProtobufDeclarationKind.MESSAGE) =
                ResolvedProtobufReference(name(target), kind)

            fun referenceField(fieldName: String, number: Int, reference: ResolvedProtobufReference) =
                ResolvedProtobufField.Reference(
                    ProtoIdentifier.parse(fieldName).getOrElse { error("not an identifier") },
                    FieldNumber.parse(number).getOrElse { error("not a field number") },
                    reference,
                    Cardinality.SINGULAR,
                )

            fun scalarField(fieldName: String, number: Int) =
                ResolvedProtobufField.Scalar(
                    ProtoIdentifier.parse(fieldName).getOrElse { error("not an identifier") },
                    FieldNumber.parse(number).getOrElse { error("not a field number") },
                    PrimitiveType.STRING,
                    Cardinality.SINGULAR,
                )

            fun cycle(vararg schemas: String) = ResolvedProtobufSchemaModel.Rejection.CircularImports(schemas.toList())

            "keeps its schemas in the order given" {
                val model =
                    ResolvedProtobufSchemaModel.of(listOf(schema("B"), schema("A", "B"))).getOrElse {
                        error("expected a model, got $it")
                    }

                model.schemas.map { it.identity.fullyQualifiedName } shouldContainExactly listOf("B", "A")
            }

            "keeps the schemas it accepted when the given list changes afterwards" {
                val schemas = mutableListOf(schema("A"))
                val model = ResolvedProtobufSchemaModel.of(schemas).getOrElse { error("expected a model, got $it") }

                schemas.add(schema("A"))

                model.schemas.map { it.identity.fullyQualifiedName } shouldContainExactly listOf("A")
            }

            "rejects a schema identity that appears more than once" {
                ResolvedProtobufSchemaModel.of(listOf(schema("a.A"), schema("B"), schema("a.A", "B"))) shouldBe
                    Either.Left(nonEmptyListOf(ResolvedProtobufSchemaModel.Rejection.DuplicateSchema(name("a.A"), 2)))
            }

            "accepts schemas that reference each other in one direction only" {
                ResolvedProtobufSchemaModel.of(listOf(schema("A", "B", "C"), schema("B", "C"), schema("C")))
                    .isRight() shouldBe true
            }

            "accepts a schema that references itself" {
                ResolvedProtobufSchemaModel.of(listOf(schema("A", "A"))).isRight() shouldBe true
            }

            "rejects two schemas that reference each other" {
                ResolvedProtobufSchemaModel.of(listOf(schema("B", "A"), schema("A", "B"))) shouldBe
                    Either.Left(nonEmptyListOf(cycle("A", "B")))
            }

            "reports each independent cycle once in name order" {
                ResolvedProtobufSchemaModel.of(
                    listOf(schema("D", "C"), schema("C", "D"), schema("B", "A"), schema("A", "B")),
                ) shouldBe Either.Left(nonEmptyListOf(cycle("A", "B"), cycle("C", "D")))
            }

            "rejects a reference to a schema that the model does not contain" {
                ResolvedProtobufSchemaModel.of(
                    listOf(messageSchema("A", listOf(referenceField("b", 1, referenceTo("p.B"))))),
                ) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.UnresolvedReference(
                                name("A"),
                                ProtobufResolutionIssue.ReferenceLocation.Field("b"),
                                name("p.B"),
                            ),
                        ),
                    )
            }

            "rejects an unresolved reference held by a map value or a oneof field" {
                val mapField =
                    ResolvedProtobufField.Map(
                        ProtoIdentifier.parse("m").getOrElse { error("not an identifier") },
                        FieldNumber.parse(1).getOrElse { error("not a field number") },
                        PrimitiveType.STRING,
                        ResolvedProtobufValueType.Reference(referenceTo("MapTarget")),
                    )
                val oneof =
                    ResolvedProtobufOneof(
                        ProtoIdentifier.parse("choice").getOrElse { error("not an identifier") },
                        nonEmptyListOf(
                            ResolvedProtobufOneof.Field(
                                ProtoIdentifier.parse("x").getOrElse { error("not an identifier") },
                                FieldNumber.parse(2).getOrElse { error("not a field number") },
                                ResolvedProtobufValueType.Reference(referenceTo("OneofTarget")),
                            ),
                        ),
                    )

                ResolvedProtobufSchemaModel.of(listOf(messageSchema("A", listOf(mapField), listOf(oneof)))) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.UnresolvedReference(
                                name("A"),
                                ProtobufResolutionIssue.ReferenceLocation.MapValue("m"),
                                name("MapTarget"),
                            ),
                            ResolvedProtobufSchemaModel.Rejection.UnresolvedReference(
                                name("A"),
                                ProtobufResolutionIssue.ReferenceLocation.OneofField("choice", "x"),
                                name("OneofTarget"),
                            ),
                        ),
                    )
            }

            "rejects a reference to an enum that is recorded as a reference to a message" {
                ResolvedProtobufSchemaModel.of(
                    listOf(
                        messageSchema("A", listOf(referenceField("e", 1, referenceTo("E")))),
                        enumSchema("E", "E_UNSPECIFIED"),
                    ),
                ) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.ReferenceKindMismatch(
                                name("A"),
                                ProtobufResolutionIssue.ReferenceLocation.Field("e"),
                                name("E"),
                                referencedAs = ProtobufDeclarationKind.MESSAGE,
                                declaredAs = ProtobufDeclarationKind.ENUM,
                            ),
                        ),
                    )
            }

            "rejects a reference to a message that is recorded as a reference to an enum" {
                ResolvedProtobufSchemaModel.of(
                    listOf(
                        messageSchema(
                            "A",
                            listOf(referenceField("b", 1, referenceTo("B", ProtobufDeclarationKind.ENUM))),
                        ),
                        messageSchema("B"),
                    ),
                ) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.ReferenceKindMismatch(
                                name("A"),
                                ProtobufResolutionIssue.ReferenceLocation.Field("b"),
                                name("B"),
                                referencedAs = ProtobufDeclarationKind.ENUM,
                                declaredAs = ProtobufDeclarationKind.MESSAGE,
                            ),
                        ),
                    )
            }

            "accepts references to an enum and to a message that match their kinds" {
                ResolvedProtobufSchemaModel.of(
                        listOf(
                            messageSchema(
                                "A",
                                listOf(
                                    referenceField("e", 1, referenceTo("E", ProtobufDeclarationKind.ENUM)),
                                    referenceField("b", 2, referenceTo("B")),
                                ),
                            ),
                            messageSchema("B"),
                            enumSchema("E", "E_UNSPECIFIED"),
                        ),
                    )
                    .isRight() shouldBe true
            }

            "rejects an enum value named like a type of the same package" {
                ResolvedProtobufSchemaModel.of(
                    listOf(enumSchema("p.Status", "UNSPECIFIED", "Contact"), messageSchema("p.Contact")),
                ) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.PackageSymbolCollision(
                                "p",
                                "Contact",
                                listOf(
                                    ProtobufResolutionIssue.SymbolDeclaration.EnumValue("p.Status", "Contact"),
                                    ProtobufResolutionIssue.SymbolDeclaration.Type("p.Contact"),
                                ),
                            ),
                        ),
                    )
            }

            "rejects the same enum value name in two enums of one package" {
                ResolvedProtobufSchemaModel.of(
                    listOf(enumSchema("E1", "UNSPECIFIED"), enumSchema("E2", "UNSPECIFIED")),
                ) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.PackageSymbolCollision(
                                "",
                                "UNSPECIFIED",
                                listOf(
                                    ProtobufResolutionIssue.SymbolDeclaration.EnumValue("E1", "UNSPECIFIED"),
                                    ProtobufResolutionIssue.SymbolDeclaration.EnumValue("E2", "UNSPECIFIED"),
                                ),
                            ),
                        ),
                    )
            }

            "rejects a type named like a sub-package" {
                ResolvedProtobufSchemaModel.of(listOf(messageSchema("p.q"), messageSchema("p.q.R"))) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufSchemaModel.Rejection.PackageSymbolCollision(
                                "p",
                                "q",
                                listOf(
                                    ProtobufResolutionIssue.SymbolDeclaration.Type("p.q"),
                                    ProtobufResolutionIssue.SymbolDeclaration.Package("p.q"),
                                ),
                            ),
                        ),
                    )
            }

            "accepts the same enum value name in enums of different packages" {
                ResolvedProtobufSchemaModel.of(
                        listOf(enumSchema("a.E", "UNSPECIFIED"), enumSchema("b.E", "UNSPECIFIED")),
                    )
                    .isRight() shouldBe true
            }

            "accepts a message named like a field of another message" {
                ResolvedProtobufSchemaModel.of(
                        listOf(
                            messageSchema("p.Contact", listOf(scalarField("Address", 1))),
                            messageSchema("p.Address"),
                        ),
                    )
                    .isRight() shouldBe true
            }

            "reports a schema identity that appears more than once without a package collision" {
                ResolvedProtobufSchemaModel.of(
                    listOf(enumSchema("p.E", "UNSPECIFIED"), enumSchema("p.E", "UNSPECIFIED")),
                ) shouldBe
                    Either.Left(nonEmptyListOf(ResolvedProtobufSchemaModel.Rejection.DuplicateSchema(name("p.E"), 2)))
            }
        },
    )
