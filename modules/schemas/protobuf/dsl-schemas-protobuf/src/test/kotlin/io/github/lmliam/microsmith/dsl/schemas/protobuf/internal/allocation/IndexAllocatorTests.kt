package io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.allocation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class IndexAllocatorTests :
    StringSpec({
        val max = 1000

        "allocate without request returns first available index >= min" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.allocate() shouldBe 1
            allocator.allocate() shouldBe 2
            allocator.allocate() shouldBe 3
        }

        "allocate starts at firstAutomatic while explicit requests may use min" {
            val allocator = IndexAllocator(min = 0, max = max, firstAutomatic = 1)
            allocator.allocate() shouldBe 1
            allocator.allocate(0) shouldBe 0
            allocator.allocate() shouldBe 2
        }

        "allocate skips used indexes" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.allocate(3)
            allocator.allocate() shouldBe 1
            allocator.allocate() shouldBe 2
            allocator.allocate() shouldBe 4
        }

        "allocate skips reserved indexes" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.reserve(1)
            allocator.allocate() shouldBe 2
        }

        "allocate skips protoReserved range" {
            val allocator = IndexAllocator(min = 1, max = max, protoReserved = 1..5)
            allocator.allocate() shouldBe 6
        }

        "allocate with explicit request returns requested index if valid" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.allocate(3) shouldBe 3
        }

        "allocate with explicit request throws if index already used" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.allocate(5)
            shouldThrow<IllegalArgumentException> { allocator.allocate(5) }
        }

        "reserve single index adds to reserved set" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.reserve(1)
            allocator.reserved() shouldBe setOf(1..1)
        }

        "reserve range adds to reserved set" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.reserve(1..3)
            allocator.reserved() shouldBe setOf(1..3)
        }

        "reserve accepts indexes inside the protoReserved range" {
            val allocator = IndexAllocator(min = 1, max = max, protoReserved = 10..20)
            allocator.reserve(15)
            allocator.reserved() shouldBe setOf(15..15)
        }

        "reserve accepts a range spanning the protoReserved range" {
            val allocator = IndexAllocator(min = 1, max = max, protoReserved = 10..20)
            allocator.reserve(8..22)
            allocator.reserved() shouldBe setOf(8..22)
        }

        "reserve merges overlapping ranges" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.reserve(1..3)
            allocator.reserve(4..10)
            allocator.reserved() shouldBe setOf(1..10)
        }

        "validate throws for index below min" {
            val allocator = IndexAllocator(min = 5, max = max)
            shouldThrow<IllegalArgumentException> { allocator.validate(4) }
        }

        "validate throws for index above max" {
            val allocator = IndexAllocator(min = 5, max = max)
            shouldThrow<IllegalArgumentException> { allocator.validate(max + 1) }
        }

        "validate throws for index in protoReserved range" {
            val allocator = IndexAllocator(min = 5, max = max, protoReserved = 1..10)
            shouldThrow<IllegalArgumentException> { allocator.validate(6) }
        }

        "validate throws for index already reserved" {
            val allocator = IndexAllocator(min = 1, max = max)
            allocator.reserve(1..10)
            shouldThrow<IllegalArgumentException> { allocator.validate(6) }
        }

        "validate throws for index already used" {
            val allocator = IndexAllocator(min = 5, max = max)
            allocator.allocate(6)
            shouldThrow<IllegalArgumentException> { allocator.validate(6) }
        }

        "reserve throws for range overlapping used indexes" {
            val allocator = IndexAllocator(min = 5, max = max)
            allocator.allocate(8)
            shouldThrow<IllegalArgumentException> { allocator.reserve(6..10) }
        }

        "reserve throws for range overlapping reserved ranges" {
            val allocator = IndexAllocator(min = 5, max = max)
            allocator.reserve(10..15)
            shouldThrow<IllegalArgumentException> { allocator.reserve(12..20) }
        }

        "multiple allocations and reservations coexist correctly" {
            val allocator = IndexAllocator(min = 1, max = max, protoReserved = 50..60)
            allocator.allocate() shouldBe 1
            allocator.allocate(10) shouldBe 10
            allocator.reserve(20..25)
            allocator.allocate() shouldBe 2
            allocator.allocate() shouldBe 3
            allocator.allocate(30) shouldBe 30
            allocator.reserved() shouldBe setOf(20..25)
        }
    })
