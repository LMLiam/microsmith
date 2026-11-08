package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.protoIdentifier
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.renderProtobufDeclaration
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtobufReservedSectionRenderingTests :
    StringSpec({
        fun renderMessage(vararg reservations: Reserved) =
            renderProtobufDeclaration(
                ResolvedProtobufMessage(
                    name = protoIdentifier("Contact"),
                    fields = emptyList(),
                    oneofs = emptyList(),
                    reservations = reservations.toList(),
                )
            )

        fun renderEnum(vararg reservations: Reserved) =
            renderProtobufDeclaration(
                ResolvedProtobufEnum(
                    name = protoIdentifier("Status"),
                    values = listOf(ResolvedProtobufEnumValue(protoIdentifier("UNSPECIFIED"), EnumValueNumber(0))),
                    reservations = reservations.toList(),
                )
            )

        "message without reservations renders no reserved statement" { renderMessage() shouldBe "message Contact {\n}" }

        "message renders names before numbers in declaration order" {
            renderMessage(
                ReservedIndex(19_500),
                ReservedName("old_b"),
                ReservedRange(18_000..20_000),
                ReservedName("old_a"),
                ReservedToMax(1_000),
            ) shouldBe
                "message Contact {\n" +
                    "  reserved \"old_b\", \"old_a\";\n" +
                    "  reserved 19500, 18000 to 20000, 1000 to max;\n" +
                    "}"
        }

        "message renders a range that is exactly the implementation range" {
            renderMessage(ReservedRange(19_000..19_999)) shouldBe "message Contact {\n  reserved 19000 to 19999;\n}"
        }

        "enum renders names before numbers in declaration order" {
            renderEnum(ReservedIndex(-5), ReservedName("OLD"), ReservedRange(-10..-8), ReservedToMax(100)) shouldBe
                "enum Status {\n" +
                    "  reserved \"OLD\";\n" +
                    "  reserved -5, -10 to -8, 100 to max;\n" +
                    "  UNSPECIFIED = 0;\n" +
                    "}"
        }
    })
