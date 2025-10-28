package me.liam.microsmith.dsl.schemas.protobuf.field

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReferenceFieldBuilderTests : StringSpec({
    "default state has null index and REQUIRED cardinality" {
        val builder = ReferenceFieldBuilder()
        builder.index shouldBe null
        builder.cardinality shouldBe Cardinality.REQUIRED
    }

    "index is set correctly" {
        val builder = ReferenceFieldBuilder()
        builder.index(5)
        builder.index shouldBe 5
    }

    "reassigning index overwrites previous value" {
        val builder = ReferenceFieldBuilder()
        builder.index(5)
        builder.index(13)
        builder.index shouldBe 13
    }

    "optional sets cardinality to OPTIONAL" {
        val builder = ReferenceFieldBuilder()
        builder.optional()
        builder.cardinality shouldBe Cardinality.OPTIONAL
    }

    "repeated sets cardinality to REPEATED" {
        val builder = ReferenceFieldBuilder()
        builder.repeated()
        builder.cardinality shouldBe Cardinality.REPEATED
    }

    "calling optional twice throws" {
        val builder = ReferenceFieldBuilder()
        builder.optional()
        shouldThrow<IllegalArgumentException> {
            builder.optional()
        }
    }

    "calling repeated twice throws" {
        val builder = ReferenceFieldBuilder()
        builder.repeated()
        shouldThrow<IllegalArgumentException> {
            builder.repeated()
        }
    }

    "calling optional then repeated throws" {
        val builder = ReferenceFieldBuilder()
        builder.optional()
        shouldThrow<IllegalArgumentException> {
            builder.repeated()
        }
    }

    "calling repeated then optional throws" {
        val builder = ReferenceFieldBuilder()
        builder.repeated()
        shouldThrow<IllegalArgumentException> {
            builder.optional()
        }
    }
})