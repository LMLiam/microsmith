package me.liam.microsmith.dsl.schemas.protobuf.types

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange

class EnumBuilderTests : StringSpec({
    "default enum contains UNSPECIFIED at index 0" {
        val builder = EnumBuilder("TestEnum")
        val enum = builder.build()
        enum.name shouldBe "TestEnum"
        enum.values.map { it.name } shouldContainExactly listOf(Enum.UNSPECIFIED)
        enum.values.map { it.index } shouldContainExactly listOf(0)
    }

    "can add value with explicit index" {
        val builder = EnumBuilder("E")
        builder.value("FOO") { index(5) }
        val enum = builder.build()
        enum.values.map { it.name } shouldContainExactly listOf(Enum.UNSPECIFIED, "FOO")
        enum.values.map { it.index } shouldContainExactly listOf(0, 5)
    }

    "auto allocates index when not provided" {
        val builder = EnumBuilder("E")
        builder.value("FOO")
        builder.value("BAR")
        val enum = builder.build()
        enum.values.map { it.name } shouldContainExactly listOf(Enum.UNSPECIFIED, "FOO", "BAR")
        enum.values.map { it.index } shouldContainExactly listOf(0, 1, 2)
    }

    "values are sorted by index in build output" {
        val builder = EnumBuilder("E")
        builder.value("FOO") { index(5) }
        builder.value("BAR") { index(1) }
        val enum = builder.build()
        enum.values.map { it.name } shouldContainExactly listOf(Enum.UNSPECIFIED, "BAR", "FOO")
        enum.values.map { it.index }.apply {
            this[0] shouldBe 0
            this[1] shouldBe 1
            this[2] shouldBe 5
        } shouldContainExactly listOf(0, 1, 5)
    }

    "duplicate value names throw" {
        val builder = EnumBuilder("E")
        builder.value("FOO")
        shouldThrow<IllegalArgumentException> {
            builder.value("FOO")
        }
    }

    "duplicate indexes throw" {
        val builder = EnumBuilder("E")
        builder.value("FOO") { index(5) }
        shouldThrow<IllegalArgumentException> {
            builder.value("BAR") { index(5) }
        }
    }

    "reserved indexes prevent allocation" {
        val builder = EnumBuilder("E")
        builder.reserved(1)
        shouldThrow<IllegalArgumentException> {
            builder.value("FOO") { index(1) }
        }
    }

    "reserved ranges prevent allocation" {
        val builder = EnumBuilder("E")
        builder.reserved(1..3)
        shouldThrow<IllegalArgumentException> {
            builder.value("FOO") { index(2) }
        }
    }

    "reserved toMax prevents allocation above threshold" {
        val builder = EnumBuilder("E")
        builder.reserved(MaxRange(100))
        shouldThrow<IllegalArgumentException> {
            builder.value("FOO") { index(101) }
        }
    }

    "reserved names prevent reuse" {
        val builder = EnumBuilder("E")
        builder.reserved("FOO")
        shouldThrow<IllegalArgumentException> {
            builder.value("FOO")
        }
    }

    "reserved block delegates correctly" {
        val builder = EnumBuilder("E")
        builder.reserved {
            index(5)
            name("X")
        }
        shouldThrow<IllegalArgumentException> {
            builder.value("FOO") { index(5) }
        }
        shouldThrow<IllegalArgumentException> {
            builder.value("X")
        }
    }

    "build collects reserved indexes and names" {
        val builder = EnumBuilder("E")
        builder.reserved(1..2)
        builder.reserved("FOO", "BAR")
        val enum = builder.build()
        enum.reserved shouldContainExactly listOf(
            ReservedRange(1..2), ReservedName("BAR"), ReservedName("FOO")
        )
    }
})