package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EnumEmissionValidatorTests :
    StringSpec({
        "enum reservations can start at the maximum integer and extend to max" {
            val declaration =
                ResolvedProtobufEnum(
                    name = "Status",
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = "UNSPECIFIED",
                                number = 0,
                            )
                        ),
                    reservations = listOf(ReservedToMax(Int.MAX_VALUE)),
                )

            validateProtobufDeclaration(declaration)
        }

        "enum reservations reject descending ranges" {
            val declaration =
                ResolvedProtobufEnum(
                    name = "Status",
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = "UNSPECIFIED",
                                number = 0,
                            )
                        ),
                    reservations = listOf(ReservedRange(-1..-10)),
                )

            val error = shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }

            error.message shouldBe "Reserved range must be ascending, but was -1..-10"
        }

        "enum reservations to max overlap reservations above the message field-number limit" {
            val declaration =
                ResolvedProtobufEnum(
                    name = "Status",
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = "UNSPECIFIED",
                                number = 0,
                            )
                        ),
                    reservations = listOf(ReservedToMax(1), ReservedIndex(Int.MAX_VALUE)),
                )

            val error = shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }

            error.message shouldBe "Enum 'Status' has overlapping reserved ranges: " + "1 to max overlaps 2147483647"
        }

        "enum reservations to max reject used numbers above the message field-number limit" {
            val declaration =
                ResolvedProtobufEnum(
                    name = "Status",
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = "UNSPECIFIED",
                                number = 0,
                            ),
                            ResolvedProtobufEnumValue(
                                name = "HIGH",
                                number = Int.MAX_VALUE,
                            ),
                        ),
                    reservations = listOf(ReservedToMax(1)),
                )

            val error = shouldThrow<IllegalArgumentException> { validateProtobufDeclaration(declaration) }

            error.message shouldBe "Enum 'Status' uses reserved numbers: 2147483647"
        }

        "enum reservations use the signed integer domain rather than message field-number restrictions" {
            val declaration =
                ResolvedProtobufEnum(
                    name = "Status",
                    values =
                        listOf(
                            ResolvedProtobufEnumValue(
                                name = "UNSPECIFIED",
                                number = 0,
                            )
                        ),
                    reservations =
                        listOf(
                            ReservedIndex(Int.MIN_VALUE),
                            ReservedRange(-10..-1),
                            ReservedIndex(19_000),
                            ReservedRange(600_000_000..700_000_000),
                            ReservedIndex(Int.MAX_VALUE),
                        ),
                )

            validateProtobufDeclaration(declaration)
        }
    })
