package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufSchemaTests :
    StringSpec(
        {
            fun identifier(value: String) =
                ProtoIdentifier.parse(value).getOrElse { error("'$value' is not an identifier") }

            fun name(value: String) = QualifiedSchemaName.parse(value).getOrElse { error("'$value' is not a name") }

            fun referenceTo(target: String) = ResolvedProtobufReference(name(target), ProtobufDeclarationKind.MESSAGE)

            fun messageReferencing(typeName: String, vararg targets: String): ResolvedProtobufMessage {
                val fields = targets.mapIndexed { index, target ->
                    ResolvedProtobufField.Reference(
                        identifier("field_$index"),
                        FieldNumber.parse(index + 1).getOrElse { error("not a field number") },
                        referenceTo(target),
                        Cardinality.SINGULAR,
                    )
                }

                return ResolvedProtobufMessage.of(
                        identifier(typeName),
                        fields,
                        emptyList(),
                        ResolvedProtobufReservations.NONE,
                    )
                    .getOrElse { error("invalid message: $it") }
            }

            "takes the package from the given name and the type name from the declaration" {
                val schema = ResolvedProtobufSchema.of(messageReferencing("Contact"), packageOf = name("pkg.sub.Other"))

                schema.identity shouldBe name("pkg.sub.Contact")
            }

            "has no package when the given name has none" {
                val schema = ResolvedProtobufSchema.of(messageReferencing("Contact"), packageOf = name("Other"))

                schema.identity shouldBe name("Contact")
            }

            "depends on the schemas its declaration references without repeats or itself" {
                val schema =
                    ResolvedProtobufSchema.of(
                        messageReferencing("Contact", "z.Zip", "a.Address", "z.Zip", "a.Contact"),
                        packageOf = name("a.Contact"),
                    )

                schema.dependencies.map { it.fullyQualifiedName } shouldContainExactly listOf("a.Address", "z.Zip")
            }

            "has no dependencies when it is an enum" {
                val declaration =
                    ResolvedProtobufEnum.of(
                            identifier("Status"),
                            nonEmptyListOf(ResolvedProtobufEnumValue(identifier("UNSPECIFIED"), EnumValueNumber(0))),
                            ResolvedProtobufReservations.NONE,
                        )
                        .getOrElse { error("invalid enum: $it") }

                ResolvedProtobufSchema.of(declaration, packageOf = name("Status")).dependencies.shouldBeEmpty()
            }

            "depends on the targets of oneof and map fields" {
                val oneof =
                    ResolvedProtobufOneof(
                        identifier("choice"),
                        nonEmptyListOf(
                            ResolvedProtobufOneof.Field(
                                identifier("address"),
                                FieldNumber.parse(1).getOrElse { error("not a field number") },
                                ResolvedProtobufValueType.Reference(referenceTo("b.Address")),
                            ),
                            ResolvedProtobufOneof.Field(
                                identifier("text"),
                                FieldNumber.parse(2).getOrElse { error("not a field number") },
                                ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                            ),
                        ),
                    )
                val map =
                    ResolvedProtobufField.Map(
                        identifier("by_id"),
                        FieldNumber.parse(3).getOrElse { error("not a field number") },
                        PrimitiveType.STRING,
                        ResolvedProtobufValueType.Reference(referenceTo("a.Zip")),
                    )
                val message =
                    ResolvedProtobufMessage.of(
                            identifier("Contact"),
                            listOf(map),
                            listOf(oneof),
                            ResolvedProtobufReservations.NONE,
                        )
                        .getOrElse { error("invalid message: $it") }

                ResolvedProtobufSchema.of(message, packageOf = name("Contact")).dependencies.map {
                    it.fullyQualifiedName
                } shouldContainExactly listOf("a.Zip", "b.Address")
            }
        },
    )
