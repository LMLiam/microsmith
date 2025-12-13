package me.liam.microsmith.dsl.schemas.protobuf.field

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ScalarFieldBuilderTests :
    StringSpec({
        "default state has null index and REQUIRED cardinality" {
            val builder = ScalarFieldBuilder()
            builder.index shouldBe null
            builder.cardinality shouldBe Cardinality.REQUIRED
        }

        "sets index correctly" {
            val builder = ScalarFieldBuilder()
            builder.index(5)
            builder.index shouldBe 5
        }

        "reassigning index overwrites previous value" {
            val builder = ScalarFieldBuilder()
            builder.index(5)
            builder.index(13)
            builder.index shouldBe 13
        }

        "optional sets cardinality to OPTIONAL" {
            val builder = ScalarFieldBuilder()
            builder.optional()
            builder.cardinality shouldBe Cardinality.OPTIONAL
        }

        "repeated sets cardinality to REPEATED" {
            val builder = ScalarFieldBuilder()
            builder.repeated()
            builder.cardinality shouldBe Cardinality.REPEATED
        }

        "calling optional twice throws" {
            val builder = ScalarFieldBuilder()
            builder.optional()
            shouldThrow<IllegalArgumentException> {
                builder.optional()
            }
        }

        "calling repeated twice throws" {
            val builder = ScalarFieldBuilder()
            builder.repeated()
            shouldThrow<IllegalArgumentException> {
                builder.repeated()
            }
        }

        "calling optional then repeated throws" {
            val builder = ScalarFieldBuilder()
            builder.optional()
            shouldThrow<IllegalArgumentException> {
                builder.repeated()
            }
        }

        "calling repeated then optional throws" {
            val builder = ScalarFieldBuilder()
            builder.repeated()
            shouldThrow<IllegalArgumentException> {
                builder.optional()
            }
        }
    })