package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

import arrow.core.Either
import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReservedSpanTests :
    StringSpec({
        val message = ReservedSpan.Domain.MESSAGE
        val enum = ReservedSpan.Domain.ENUM

        "single accepts numbers in the range that implementations reserve" {
            ReservedSpan.single(19_500, message).map { it.notation } shouldBe Either.Right("19500")
        }

        "message span rejects zero and numbers above the largest field number" {
            ReservedSpan.single(0, message) shouldBe Either.Left(nonEmptyListOf(ReservedSpan.Rejection.OutOfDomain(0)))
            ReservedSpan.single(536_870_912, message) shouldBe
                Either.Left(nonEmptyListOf(ReservedSpan.Rejection.OutOfDomain(536_870_912)))
        }

        "message range rejects both endpoints that are out of bounds" {
            ReservedSpan.range(0, 536_870_912, message) shouldBe
                Either.Left(
                    nonEmptyListOf(
                        ReservedSpan.Rejection.OutOfDomain(0),
                        ReservedSpan.Rejection.OutOfDomain(536_870_912),
                    )
                )
        }

        "range rejects a descending pair without checking the bounds" {
            ReservedSpan.range(20, 10, message) shouldBe
                Either.Left(nonEmptyListOf(ReservedSpan.Rejection.Descending(20, 10)))
            ReservedSpan.range(0, -1, message) shouldBe
                Either.Left(nonEmptyListOf(ReservedSpan.Rejection.Descending(0, -1)))
        }

        "message range to max stops at the largest field number" {
            ReservedSpan.toMax(1_000, message).map { it.last } shouldBe Either.Right(536_870_911)
        }

        "enum range to max stops at the largest integer" {
            ReservedSpan.toMax(1, enum).map { it.last } shouldBe Either.Right(Int.MAX_VALUE)
        }

        "message range to max rejects a start above the largest field number" {
            ReservedSpan.toMax(536_870_912, message) shouldBe
                Either.Left(nonEmptyListOf(ReservedSpan.Rejection.OutOfDomain(536_870_912)))
        }

        "enum span accepts negative numbers and the smallest integer" {
            ReservedSpan.single(Int.MIN_VALUE, enum).isRight() shouldBe true
            ReservedSpan.range(-10, -1, enum).isRight() shouldBe true
        }

        "notation keeps the form a reservation was written in" {
            ReservedSpan.single(5, message).map { it.notation } shouldBe Either.Right("5")
            ReservedSpan.range(5, 5, message).map { it.notation } shouldBe Either.Right("5 to 5")
            ReservedSpan.range(5, 9, message).map { it.notation } shouldBe Either.Right("5 to 9")
            ReservedSpan.toMax(5, message).map { it.notation } shouldBe Either.Right("5 to max")
        }

        "span contains every number from first to last" {
            val contains = ReservedSpan.range(10, 20, message).map { span -> listOf(9, 10, 20, 21).map { it in span } }

            contains shouldBe Either.Right(listOf(false, true, true, false))
        }
    })
