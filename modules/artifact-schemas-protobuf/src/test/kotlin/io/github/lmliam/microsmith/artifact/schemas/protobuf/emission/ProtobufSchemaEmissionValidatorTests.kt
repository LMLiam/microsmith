package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class ProtobufSchemaEmissionValidatorTests :
    StringSpec({
        "validate rejects schema and declaration name mismatches" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Profile",
                        fields = listOf(ScalarField("value", 1, PrimitiveType.STRING)),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }

        "validate rejects duplicate field names across fields and oneofs" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Contact",
                        fields = listOf(ScalarField("value", 1, PrimitiveType.STRING)),
                        oneofs = listOf(Oneof("channel", listOf(OneofField("value", 2, PrimitiveType.STRING)))),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }

        "validate rejects duplicate field numbers across fields and oneofs" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Contact",
                        fields = listOf(ScalarField("primary", 1, PrimitiveType.STRING)),
                        oneofs = listOf(Oneof("channel", listOf(OneofField("secondary", 1, PrimitiveType.STRING)))),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }

        "validate rejects duplicate oneof names" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Contact",
                        oneofs =
                        listOf(
                            Oneof("channel", listOf(OneofField("email", 1, PrimitiveType.STRING))),
                            Oneof("channel", listOf(OneofField("phone", 2, PrimitiveType.STRING))),
                        ),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }

        "validate rejects reserved name and index collisions with used fields" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Contact",
                        fields = listOf(ScalarField("legacy_name", 10, PrimitiveType.STRING)),
                        reserved = listOf(ReservedName("legacy_name"), ReservedIndex(10)),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }

        "validate rejects overlapping reserved numeric ranges" {
            val schema =
                ProtobufSchema(
                    "pkg.Contact",
                    Message(
                        name = "Contact",
                        reserved = listOf(ReservedIndex(10), ReservedRange(10..20)),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }

        "validate rejects reserved enum names that collide with values" {
            val schema =
                ProtobufSchema(
                    "pkg.Status",
                    Enum(
                        name = "Status",
                        values = listOf(EnumValue("UNSPECIFIED", 0), EnumValue("ACTIVE", 1)),
                        reserved = listOf(ReservedName("ACTIVE")),
                    ),
                )

            shouldThrow<IllegalArgumentException> {
                ProtobufSchemaEmissionValidator.validate(schema)
            }
        }
    })
