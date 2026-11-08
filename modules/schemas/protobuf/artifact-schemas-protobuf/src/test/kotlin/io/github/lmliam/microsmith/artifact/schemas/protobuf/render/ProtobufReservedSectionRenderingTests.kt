package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.protoIdentifier
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.rangeSpan
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.renderProtobufDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.singleSpan
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.toMaxSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtobufReservedSectionRenderingTests :
    StringSpec({
        fun renderMessage(reservations: ResolvedProtobufReservations = ResolvedProtobufReservations()) =
            renderProtobufDeclaration(
                ResolvedProtobufMessage(
                    name = protoIdentifier("Contact"),
                    fields = emptyList(),
                    oneofs = emptyList(),
                    reservations = reservations,
                )
            )

        fun renderEnum(reservations: ResolvedProtobufReservations) =
            renderProtobufDeclaration(
                ResolvedProtobufEnum(
                    name = protoIdentifier("Status"),
                    values = listOf(ResolvedProtobufEnumValue(protoIdentifier("UNSPECIFIED"), EnumValueNumber(0))),
                    reservations = reservations,
                )
            )

        "message without reservations renders no reserved statement" { renderMessage() shouldBe "message Contact {\n}" }

        "message renders names before numbers" {
            renderMessage(
                ResolvedProtobufReservations(
                    names = listOf(protoIdentifier("old_b"), protoIdentifier("old_a")),
                    spans = listOf(singleSpan(19_500), rangeSpan(18_000, 20_000), toMaxSpan(1_000)),
                )
            ) shouldBe
                "message Contact {\n" +
                    "  reserved \"old_b\", \"old_a\";\n" +
                    "  reserved 19500, 18000 to 20000, 1000 to max;\n" +
                    "}"
        }

        "message renders a range that is exactly the implementation range" {
            renderMessage(ResolvedProtobufReservations(spans = listOf(rangeSpan(19_000, 19_999)))) shouldBe
                "message Contact {\n  reserved 19000 to 19999;\n}"
        }

        "enum renders names before numbers" {
            val enumDomain = ReservedSpan.Domain.ENUM

            renderEnum(
                ResolvedProtobufReservations(
                    names = listOf(protoIdentifier("OLD")),
                    spans =
                        listOf(
                            singleSpan(-5, enumDomain),
                            rangeSpan(-10, -8, enumDomain),
                            toMaxSpan(100, enumDomain),
                        ),
                )
            ) shouldBe
                "enum Status {\n" +
                    "  reserved \"OLD\";\n" +
                    "  reserved -5, -10 to -8, 100 to max;\n" +
                    "  UNSPECIFIED = 0;\n" +
                    "}"
        }
    })
