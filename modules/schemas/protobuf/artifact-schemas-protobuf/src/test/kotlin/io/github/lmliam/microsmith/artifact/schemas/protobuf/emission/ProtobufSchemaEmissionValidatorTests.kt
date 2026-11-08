package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufValueType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class ProtobufSchemaEmissionValidatorTests :
    StringSpec({
        "validate rejects duplicate field names across fields and oneofs" {
            val declaration =
                ResolvedProtobufMessage(
                    name = protoIdentifier("Contact"),
                    fields =
                        listOf(
                            ResolvedProtobufField.Scalar(
                                name = protoIdentifier("value"),
                                number = fieldNumber(1),
                                type = PrimitiveType.STRING,
                                cardinality = Cardinality.SINGULAR,
                            )
                        ),
                    oneofs =
                        listOf(
                            ResolvedProtobufOneof(
                                name = protoIdentifier("channel"),
                                fields =
                                    nonEmptyListOf(
                                        ResolvedProtobufOneof.Field(
                                            name = protoIdentifier("value"),
                                            number = fieldNumber(2),
                                            type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                        )
                                    ),
                            )
                        ),
                    reservations = ResolvedProtobufReservations.NONE,
                )

            shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }
        }

        "validate rejects duplicate field numbers across fields and oneofs" {
            val declaration =
                ResolvedProtobufMessage(
                    name = protoIdentifier("Contact"),
                    fields =
                        listOf(
                            ResolvedProtobufField.Scalar(
                                name = protoIdentifier("primary"),
                                number = fieldNumber(1),
                                type = PrimitiveType.STRING,
                                cardinality = Cardinality.SINGULAR,
                            )
                        ),
                    oneofs =
                        listOf(
                            ResolvedProtobufOneof(
                                name = protoIdentifier("channel"),
                                fields =
                                    nonEmptyListOf(
                                        ResolvedProtobufOneof.Field(
                                            name = protoIdentifier("secondary"),
                                            number = fieldNumber(1),
                                            type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                        )
                                    ),
                            )
                        ),
                    reservations = ResolvedProtobufReservations.NONE,
                )

            shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }
        }

        "validate rejects duplicate oneof names" {
            val declaration =
                ResolvedProtobufMessage(
                    name = protoIdentifier("Contact"),
                    fields = emptyList(),
                    oneofs =
                        listOf(
                            ResolvedProtobufOneof(
                                name = protoIdentifier("channel"),
                                fields =
                                    nonEmptyListOf(
                                        ResolvedProtobufOneof.Field(
                                            name = protoIdentifier("email"),
                                            number = fieldNumber(1),
                                            type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                        )
                                    ),
                            ),
                            ResolvedProtobufOneof(
                                name = protoIdentifier("channel"),
                                fields =
                                    nonEmptyListOf(
                                        ResolvedProtobufOneof.Field(
                                            name = protoIdentifier("phone"),
                                            number = fieldNumber(2),
                                            type = ResolvedProtobufValueType.Primitive(PrimitiveType.STRING),
                                        )
                                    ),
                            ),
                        ),
                    reservations = ResolvedProtobufReservations.NONE,
                )

            shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }
        }

        "validate rejects reserved name and number collisions with used fields" {
            val declaration =
                ResolvedProtobufMessage(
                    name = protoIdentifier("Contact"),
                    fields =
                        listOf(
                            ResolvedProtobufField.Scalar(
                                name = protoIdentifier("legacy_name"),
                                number = fieldNumber(10),
                                type = PrimitiveType.STRING,
                                cardinality = Cardinality.SINGULAR,
                            )
                        ),
                    oneofs = emptyList(),
                    reservations =
                        reservations(
                            names = listOf(protoIdentifier("legacy_name")),
                            spans = listOf(singleSpan(10)),
                        ),
                )

            shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }
        }
    })
