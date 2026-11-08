package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.enumDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.messageDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.protoIdentifier
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.rangeSpan
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.renderProtobufDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.reservations
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.singleSpan
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.toMaxSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ProtobufReservedSectionRenderingTests :
    StringSpec({
        fun renderMessage(reservations: ResolvedProtobufReservations = ResolvedProtobufReservations.NONE) =
            renderProtobufDeclaration(messageDeclaration(name = "Contact", reservations = reservations))

        fun renderEnum(reservations: ResolvedProtobufReservations) =
            renderProtobufDeclaration(
                enumDeclaration(
                    name = "Status",
                    values =
                        nonEmptyListOf(ResolvedProtobufEnumValue(protoIdentifier("UNSPECIFIED"), EnumValueNumber(0))),
                    reservations = reservations,
                )
            )

        "message without reservations renders no reserved statement" { renderMessage() shouldBe "message Contact {\n}" }

        "message renders names before numbers" {
            renderMessage(
                reservations(
                    names = listOf(protoIdentifier("old_b"), protoIdentifier("old_a")),
                    spans = listOf(rangeSpan(1_000, 2_000), singleSpan(19_500), toMaxSpan(1_000_000)),
                )
            ) shouldBe
                "message Contact {\n" +
                    "  reserved \"old_b\", \"old_a\";\n" +
                    "  reserved 1000 to 2000, 19500, 1000000 to max;\n" +
                    "}"
        }

        "message renders a range that is exactly the implementation range" {
            renderMessage(reservations(spans = listOf(rangeSpan(19_000, 19_999)))) shouldBe
                "message Contact {\n  reserved 19000 to 19999;\n}"
        }

        "enum renders names before numbers" {
            val enumDomain = ReservedSpan.Domain.ENUM

            renderEnum(
                reservations(
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
