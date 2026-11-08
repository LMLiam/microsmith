package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnumEmissionValidatorTests :
    StringSpec({
        val enumDomain = ReservedSpan.Domain.ENUM

        "enum reservations can start at the maximum integer and extend to max" {
            val declaration =
                ResolvedProtobufEnum(
                    name = protoIdentifier("Status"),
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = protoIdentifier("UNSPECIFIED"),
                                number = EnumValueNumber(0),
                            )
                        ),
                    reservations = reservations(spans = listOf(toMaxSpan(Int.MAX_VALUE, enumDomain))),
                )

            validateProtobufDeclaration(declaration)
        }

        "enum reservations to max reject used numbers above the message field-number limit" {
            val declaration =
                ResolvedProtobufEnum(
                    name = protoIdentifier("Status"),
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = protoIdentifier("UNSPECIFIED"),
                                number = EnumValueNumber(0),
                            ),
                            ResolvedProtobufEnumValue(
                                name = protoIdentifier("HIGH"),
                                number = EnumValueNumber(Int.MAX_VALUE),
                            ),
                        ),
                    reservations = reservations(spans = listOf(toMaxSpan(1, enumDomain))),
                )

            val error = shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }

            error.message shouldBe "Enum 'Status' uses reserved numbers: 2147483647"
        }

        "enum reservations use the signed integer domain rather than message field-number restrictions" {
            val declaration =
                ResolvedProtobufEnum(
                    name = protoIdentifier("Status"),
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = protoIdentifier("UNSPECIFIED"),
                                number = EnumValueNumber(0),
                            )
                        ),
                    reservations =
                        reservations(
                            spans =
                                listOf(
                                    singleSpan(Int.MIN_VALUE, enumDomain),
                                    rangeSpan(-10, -1, enumDomain),
                                    singleSpan(19_000, enumDomain),
                                    rangeSpan(600_000_000, 700_000_000, enumDomain),
                                    singleSpan(Int.MAX_VALUE, enumDomain),
                                )
                        ),
                )

            validateProtobufDeclaration(declaration)
        }
    })
