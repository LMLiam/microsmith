package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufEnumTests :
    StringSpec({
        fun identifier(value: String) =
            ProtoIdentifier.parse(value).getOrElse { error("'$value' is not an identifier") }

        fun value(name: String, number: Int) = ResolvedProtobufEnumValue(identifier(name), EnumValueNumber(number))

        fun reservations(names: List<String> = emptyList(), spans: List<ReservedSpan> = emptyList()) =
            ResolvedProtobufReservations.of(names.map(::identifier), spans).getOrElse { error("invalid: $it") }

        fun toMax(first: Int) =
            ReservedSpan.toMax(first, ReservedSpan.Domain.ENUM).getOrElse { error("$first is not reservable") }

        fun single(number: Int) =
            ReservedSpan.single(number, ReservedSpan.Domain.ENUM).getOrElse { error("$number is not reservable") }

        fun enumOf(
            values: NonEmptyList<ResolvedProtobufEnumValue>,
            reservations: ResolvedProtobufReservations = ResolvedProtobufReservations.NONE,
        ) = ResolvedProtobufEnum.of(identifier("Status"), values, reservations)

        "keeps its values in the order given" {
            val declaration =
                enumOf(nonEmptyListOf(value("UNSPECIFIED", 0), value("ACTIVE", 2), value("PAUSED", 1))).getOrElse {
                    error("expected an enum, got $it")
                }

            declaration.name.value shouldBe "Status"
            declaration.values.map { it.name.value } shouldContainExactly listOf("UNSPECIFIED", "ACTIVE", "PAUSED")
        }

        "rejects a first value that is not zero" {
            enumOf(nonEmptyListOf(value("ACTIVE", 1), value("UNSPECIFIED", 0))) shouldBe
                Either.Left(nonEmptyListOf(ResolvedProtobufRejection.EnumFirstValueMustBeZero("ACTIVE", 1)))
        }

        "rejects a value name that is used twice" {
            enumOf(nonEmptyListOf(value("UNSPECIFIED", 0), value("ACTIVE", 1), value("ACTIVE", 2))) shouldBe
                Either.Left(nonEmptyListOf(ResolvedProtobufRejection.DuplicateEnumValueNames(listOf("ACTIVE"))))
        }

        "rejects a value number that is used twice" {
            enumOf(nonEmptyListOf(value("UNSPECIFIED", 0), value("ACTIVE", 1), value("PAUSED", 1))) shouldBe
                Either.Left(nonEmptyListOf(ResolvedProtobufRejection.DuplicateEnumValueNumbers(listOf(1))))
        }

        "rejects a value whose name is reserved" {
            enumOf(
                nonEmptyListOf(value("UNSPECIFIED", 0), value("ACTIVE", 1)),
                reservations(names = listOf("ACTIVE")),
            ) shouldBe Either.Left(nonEmptyListOf(ResolvedProtobufRejection.ReservedNameCollision(listOf("ACTIVE"))))
        }

        "rejects a value whose number is reserved up to the largest integer" {
            enumOf(
                nonEmptyListOf(value("UNSPECIFIED", 0), value("HIGH", Int.MAX_VALUE)),
                reservations(spans = listOf(toMax(1))),
            ) shouldBe
                Either.Left(nonEmptyListOf(ResolvedProtobufRejection.ReservedNumberCollision(listOf(Int.MAX_VALUE))))
        }

        "accepts values that sit outside every reserved number" {
            enumOf(
                    nonEmptyListOf(value("UNSPECIFIED", 0), value("LOW", -3)),
                    reservations(spans = listOf(single(-5), toMax(100))),
                )
                .isRight() shouldBe true
        }

        "rejects value names that collide once the enum name prefix is stripped" {
            enumOf(
                nonEmptyListOf(
                    value("STATUS_UNSPECIFIED", 0),
                    value("STATUS_ACTIVE", 1),
                    value("ACTIVE", 2),
                    value("Active", 3),
                )
            ) shouldBe
                Either.Left(
                    nonEmptyListOf(
                        ResolvedProtobufRejection.EnumValueNameCollision(listOf("STATUS_ACTIVE", "ACTIVE", "Active"))
                    )
                )
        }

        "accepts value names that stay distinct once the enum name prefix is stripped" {
            enumOf(nonEmptyListOf(value("STATUS_UNSPECIFIED", 0), value("STATUS_ACTIVE", 1), value("PAUSED", 2)))
                .isRight() shouldBe true
        }

        "reports every rejection in the order the resolver reports issues" {
            enumOf(
                nonEmptyListOf(value("ACTIVE", 1), value("ACTIVE", 1)),
                reservations(names = listOf("ACTIVE"), spans = listOf(single(1))),
            ) shouldBe
                Either.Left(
                    nonEmptyListOf(
                        ResolvedProtobufRejection.EnumFirstValueMustBeZero("ACTIVE", 1),
                        ResolvedProtobufRejection.DuplicateEnumValueNames(listOf("ACTIVE")),
                        ResolvedProtobufRejection.DuplicateEnumValueNumbers(listOf(1)),
                        ResolvedProtobufRejection.ReservedNameCollision(listOf("ACTIVE")),
                        ResolvedProtobufRejection.ReservedNumberCollision(listOf(1)),
                    )
                )
        }
    })
