package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

import arrow.core.Either
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FieldNumberTests :
    StringSpec({
        "parse keeps the smallest and the largest field number" {
            FieldNumber.parse(1).map(FieldNumber::value) shouldBe Either.Right(1)
            FieldNumber.parse(536_870_911).map(FieldNumber::value) shouldBe Either.Right(536_870_911)
        }

        "parse rejects zero and numbers above the largest field number" {
            FieldNumber.parse(0) shouldBe Either.Left(0)
            FieldNumber.parse(536_870_912) shouldBe Either.Left(536_870_912)
        }

        "parse rejects the range that protobuf implementations reserve" {
            FieldNumber.parse(19_000) shouldBe Either.Left(19_000)
            FieldNumber.parse(19_999) shouldBe Either.Left(19_999)
        }

        "parse keeps the numbers next to the range that protobuf implementations reserve" {
            FieldNumber.parse(18_999).map(FieldNumber::value) shouldBe Either.Right(18_999)
            FieldNumber.parse(20_000).map(FieldNumber::value) shouldBe Either.Right(20_000)
        }
    })
