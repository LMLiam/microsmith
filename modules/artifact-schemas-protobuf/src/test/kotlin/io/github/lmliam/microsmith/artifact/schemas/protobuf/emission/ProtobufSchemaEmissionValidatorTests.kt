package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufValueType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class ProtobufSchemaEmissionValidatorTests :
    StringSpec({
        "validate rejects duplicate field names across fields and oneofs" {
            val declaration = ResolvedProtobufMessage(
                name = "Contact",
                fields = listOf(
                    ResolvedProtobufField.Scalar(
                        name = "value",
                        number = 1,
                        type = PrimitiveType.STRING,
                        cardinality = Cardinality.SINGULAR,
                    ),
                ),
                oneofs = listOf(
                    ResolvedProtobufOneof(
                        name = "channel",
                        fields = listOf(
                            ResolvedProtobufOneof.Field(
                                name = "value",
                                number = 2,
                                type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                            ),
                        ),
                    ),
                ),
                reservations = emptyList(),
            )

            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(declaration)
            }
        }

        "validate rejects duplicate field numbers across fields and oneofs" {
            val declaration =
                ResolvedProtobufMessage(
                    name = "Contact",
                    fields = listOf(
                        ResolvedProtobufField.Scalar(
                            name = "primary",
                            number = 1,
                            type = PrimitiveType.STRING,
                            cardinality = Cardinality.SINGULAR,
                        ),
                    ),
                    oneofs = listOf(
                        ResolvedProtobufOneof(
                            name = "channel",
                            fields = listOf(
                                ResolvedProtobufOneof.Field(
                                    name = "secondary",
                                    number = 1,
                                    type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                ),
                            ),
                        ),
                    ),
                    reservations = emptyList(),
                )

            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(declaration)
            }
        }

        "validate rejects duplicate oneof names" {
            val declaration =
                ResolvedProtobufMessage(
                    name = "Contact",
                    fields = emptyList(),
                    oneofs = listOf(
                        ResolvedProtobufOneof(
                            name = "channel",
                            fields = listOf(
                                ResolvedProtobufOneof.Field(
                                    name = "email",
                                    number = 1,
                                    type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                ),
                            ),
                        ),
                        ResolvedProtobufOneof(
                            name = "channel",
                            fields =
                            listOf(
                                ResolvedProtobufOneof.Field(
                                    name = "phone",
                                    number = 2,
                                    type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                ),
                            ),
                        ),
                    ),
                    reservations = emptyList(),
                )

            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(declaration)
            }
        }

        "validate rejects overlapping reserved numeric ranges" {
            val declaration =
                ResolvedProtobufMessage(
                    name = "Contact",
                    fields = emptyList(),
                    oneofs = emptyList(),
                    reservations = listOf(
                        ReservedIndex(10),
                        ReservedRange(10..20),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(declaration)
            }
        }

        "validate rejects reserved enum names that collide with values" {
            val declaration =
                ResolvedProtobufEnum(
                    name = "Status",
                    values = listOf(
                        ResolvedProtobufEnumValue(
                            name = "UNSPECIFIED",
                            number = 0,
                        ),
                        ResolvedProtobufEnumValue(
                            name = "ACTIVE",
                            number = 1,
                        ),
                    ),
                    reservations = listOf(ReservedName("ACTIVE")),
                )

            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(declaration)
            }
        }

        "validate rejects reserved name and number collisions with used fields" {
            val declaration =
                ResolvedProtobufMessage(
                    name = "Contact",
                    fields = listOf(
                        ResolvedProtobufField.Scalar(
                            name = "legacy_name",
                            number = 10,
                            type = PrimitiveType.STRING,
                            cardinality = Cardinality.SINGULAR,
                        ),
                    ),
                    oneofs = emptyList(),
                    reservations = listOf(
                        ReservedName("legacy_name"),
                        ReservedIndex(10),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                validateProtobufDeclaration(declaration)
            }
        }
    })
