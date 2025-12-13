package me.liam.microsmith.dsl.schemas.protobuf.extensions

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IntRangeExtensionsTests :
    StringSpec({
        "merges disjoint ranges" {
            val set = mutableSetOf(1..5)
            set.merge(10..15)
            set shouldBe setOf(1..5, 10..15)
        }

        "merges overlapping ranges" {
            val set = mutableSetOf(1..5)
            set.merge(4..10)
            set shouldBe setOf(1..10)
        }

        "merge adjacent ranges" {
            val set = mutableSetOf(1..5)
            set.merge(6..8)
            set shouldBe setOf(1..8)
        }

        "nested range does not expand" {
            val set = mutableSetOf(1..10)
            set.merge(3..5)
            set shouldBe setOf(1..10)
        }

        "merges multiple ranges into one" {
            val set = mutableSetOf(1..5, 10..15)
            set.merge(4..12)
            set shouldBe setOf(1..15)
        }

        "chain merge across three ranges" {
            val set = mutableSetOf(1..5, 7..9, 11..13)
            set.merge(5..11)
            set shouldBe setOf(1..13)
        }
    })