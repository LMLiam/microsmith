package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ResolvedProtobufReservationsTests :
    StringSpec(
        {
            fun identifier(value: String) =
                ProtoIdentifier.parse(value).getOrElse { error("'$value' is not an identifier") }

            fun single(number: Int) =
                ReservedSpan.single(number, ReservedSpan.Domain.MESSAGE).getOrElse {
                    error("$number is not reservable")
                }

            fun range(first: Int, last: Int) =
                ReservedSpan.range(first, last, ReservedSpan.Domain.MESSAGE).getOrElse {
                    error("$first..$last is not reservable")
                }

            "keeps distinct names and disjoint spans in the order given" {
                val reservations =
                    ResolvedProtobufReservations.of(
                            names = listOf(identifier("old_b"), identifier("old_a")),
                            spans = listOf(range(10, 20), single(5)),
                        )
                        .getOrElse { error("expected reservations, got $it") }

                reservations.names.map { it.value } shouldContainExactly listOf("old_b", "old_a")
                reservations.spans.map { it.notation } shouldContainExactly listOf("10 to 20", "5")
            }

            "keeps the names and spans it accepted when the given lists change afterwards" {
                val names = mutableListOf(identifier("a"))
                val spans = mutableListOf(single(1))
                val reservations =
                    ResolvedProtobufReservations.of(names, spans).getOrElse { error("expected reservations, got $it") }

                names.add(identifier("a"))
                spans.add(single(1))

                reservations.names.map { it.value } shouldContainExactly listOf("a")
                reservations.spans.map { it.notation } shouldContainExactly listOf("1")
            }

            "rejects a name that is reserved twice" {
                ResolvedProtobufReservations.of(
                    listOf(identifier("b"), identifier("a"), identifier("a")),
                    emptyList(),
                ) shouldBe Either.Left(nonEmptyListOf(ResolvedProtobufRejection.DuplicateReservedNames(listOf("a"))))
            }

            "rejects spans that overlap" {
                ResolvedProtobufReservations.of(emptyList(), listOf(range(10, 20), single(20))) shouldBe
                    Either.Left(nonEmptyListOf(ResolvedProtobufRejection.OverlappingReservedNumbers))
            }

            "reports duplicate names before overlapping spans" {
                ResolvedProtobufReservations.of(
                    listOf(identifier("a"), identifier("a")),
                    listOf(single(1), single(1)),
                ) shouldBe
                    Either.Left(
                        nonEmptyListOf(
                            ResolvedProtobufRejection.DuplicateReservedNames(listOf("a")),
                            ResolvedProtobufRejection.OverlappingReservedNumbers,
                        ),
                    )
            }
        },
    )
