package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class EnumBuilderTests :
    StringSpec({
        "default enum contains the prefixed unspecified value at index 0" {
            val builder = EnumBuilder("TestEnum")
            val enum = builder.build()
            enum.name shouldBe "TestEnum"
            enum.values.map { it.name } shouldContainExactly listOf("TEST_ENUM_UNSPECIFIED")
            enum.values.map { it.index } shouldContainExactly listOf(0)
        }

        listOf(
                "E" to "E_UNSPECIFIED",
                "Status" to "STATUS_UNSPECIFIED",
                "OrderStatus" to "ORDER_STATUS_UNSPECIFIED",
                "HTTPStatus" to "HTTP_STATUS_UNSPECIFIED",
                "Http2Status" to "HTTP2_STATUS_UNSPECIFIED",
                "Level2" to "LEVEL2_UNSPECIFIED",
                "order_status" to "ORDER_STATUS_UNSPECIFIED",
                "ORDER_STATUS" to "ORDER_STATUS_UNSPECIFIED",
            )
            .forEach { (enumName, expected) ->
                "default value of enum $enumName is $expected" {
                    EnumBuilder(enumName).build().values.map { it.name } shouldContainExactly listOf(expected)
                }
            }

        "can add value with explicit index" {
            val builder = EnumBuilder("E")
            builder.value("FOO") { index(5) }
            val enum = builder.build()
            enum.values.map { it.name } shouldContainExactly listOf("E_UNSPECIFIED", "FOO")
            enum.values.map { it.index } shouldContainExactly listOf(0, 5)
        }

        "auto allocates index when not provided" {
            val builder = EnumBuilder("E")
            builder.value("FOO")
            builder.value("BAR")
            val enum = builder.build()
            enum.values.map { it.name } shouldContainExactly listOf("E_UNSPECIFIED", "FOO", "BAR")
            enum.values.map { it.index } shouldContainExactly listOf(0, 1, 2)
        }

        "values are sorted by index in build output" {
            val builder = EnumBuilder("E")
            builder.value("FOO") { index(5) }
            builder.value("BAR") { index(1) }
            val enum = builder.build()
            enum.values.map { it.name } shouldContainExactly listOf("E_UNSPECIFIED", "BAR", "FOO")
            enum.values
                .map { it.index }
                .apply {
                    this[0] shouldBe 0
                    this[1] shouldBe 1
                    this[2] shouldBe 5
                } shouldContainExactly listOf(0, 1, 5)
        }

        "duplicate value names throw" {
            val builder = EnumBuilder("E")
            builder.value("FOO")
            shouldThrow<IllegalArgumentException> { builder.value("FOO") }
        }

        "duplicate indexes throw" {
            val builder = EnumBuilder("E")
            builder.value("FOO") { index(5) }
            shouldThrow<IllegalArgumentException> { builder.value("BAR") { index(5) } }
        }

        "reserved indexes prevent allocation" {
            val builder = EnumBuilder("E")
            builder.reserved(1)
            shouldThrow<IllegalArgumentException> { builder.value("FOO") { index(1) } }
        }

        "reserved ranges prevent allocation" {
            val builder = EnumBuilder("E")
            builder.reserved(1..3)
            shouldThrow<IllegalArgumentException> { builder.value("FOO") { index(2) } }
        }

        "reserved toMax prevents allocation above threshold" {
            val builder = EnumBuilder("E")
            builder.reserved(MaxRange(100))
            shouldThrow<IllegalArgumentException> { builder.value("FOO") { index(101) } }
        }

        "reserved names prevent reuse" {
            val builder = EnumBuilder("E")
            builder.reserved("FOO")
            shouldThrow<IllegalArgumentException> { builder.value("FOO") }
        }

        "reserved block delegates correctly" {
            val builder = EnumBuilder("E")
            builder.reserved {
                index(5)
                name("X")
            }
            shouldThrow<IllegalArgumentException> { builder.value("FOO") { index(5) } }
            shouldThrow<IllegalArgumentException> { builder.value("X") }
        }

        "build collects reserved indexes and names" {
            val builder = EnumBuilder("E")
            builder.reserved(1..2)
            builder.reserved("FOO", "BAR")
            val enum = builder.build()
            enum.reserved shouldContainExactly
                listOf(
                    ReservedRange(1..2),
                    ReservedName("BAR"),
                    ReservedName("FOO"),
                )
        }

        "a failed explicit index leaves the value name free for a retry" {
            val builder = EnumBuilder("E")
            builder.reserved(1)
            shouldThrow<IllegalArgumentException> { builder.value("FOO") { index(1) } }
            builder.value("FOO") { index(2) }
            builder.build().values.map { it.name } shouldContainExactly listOf("E_UNSPECIFIED", "FOO")
        }

        "a value declared at index 0 replaces the default zero value" {
            val builder = EnumBuilder("E")
            builder.value("OK") { index(0) }
            builder.value("FAILED")
            val enum = builder.build()
            enum.values.map { it.name } shouldContainExactly listOf("OK", "FAILED")
            enum.values.map { it.index } shouldContainExactly listOf(0, 1)
        }

        "a value without an index never takes zero before an explicit zero is declared" {
            val builder = EnumBuilder("E")
            builder.value("FAILED")
            builder.value("OK") { index(0) }
            val enum = builder.build()
            enum.values.map { it.name } shouldContainExactly listOf("OK", "FAILED")
            enum.values.map { it.index } shouldContainExactly listOf(0, 1)
        }

        "values are sorted by index when the explicit zero is declared last" {
            val builder = EnumBuilder("E")
            builder.value("FAILED") { index(5) }
            builder.value("OK") { index(0) }
            builder.build().values.map { it.index } shouldContainExactly listOf(0, 5)
        }

        "the explicit zero value may use the name of the default zero value" {
            val builder = EnumBuilder("OrderStatus")
            builder.value("ORDER_STATUS_UNSPECIFIED") { index(0) }
            builder.build().values.map { it.name } shouldContainExactly listOf("ORDER_STATUS_UNSPECIFIED")
        }

        "building twice produces the same enum" {
            val builder = EnumBuilder("E")
            builder.value("FOO")
            builder.build() shouldBe builder.build()
        }

        "a value named like the default zero value is rejected when the default zero value is needed" {
            val builder = EnumBuilder("OrderStatus")
            builder.value("ORDER_STATUS_UNSPECIFIED") { index(5) }
            shouldThrow<IllegalArgumentException> { builder.build() }.message shouldBe
                "Name already used: ORDER_STATUS_UNSPECIFIED (the default zero value of enum OrderStatus)"
        }

        "a reserved name equal to the default zero value is rejected when the default zero value is needed" {
            val builder = EnumBuilder("OrderStatus")
            builder.reserved("ORDER_STATUS_UNSPECIFIED")
            builder.value("FOO")
            shouldThrow<IllegalArgumentException> { builder.build() }.message shouldBe
                "Name already reserved: ORDER_STATUS_UNSPECIFIED (the default zero value of enum OrderStatus)"
        }

        "a reserved name equal to the default zero value is accepted when an explicit zero value exists" {
            val builder = EnumBuilder("OrderStatus")
            builder.reserved("ORDER_STATUS_UNSPECIFIED")
            builder.value("OK") { index(0) }
            builder.build().values.map { it.name } shouldContainExactly listOf("OK")
        }

        "reserving index 0 without declaring a zero value generates no default zero value" {
            val builder = EnumBuilder("E")
            builder.reserved(0)
            builder.value("FOO")
            val enum = builder.build()
            enum.values.map { it.name } shouldContainExactly listOf("FOO")
            enum.values.map { it.index } shouldContainExactly listOf(1)
        }

        "a value cannot take index 0 once it is reserved" {
            val builder = EnumBuilder("E")
            builder.reserved(0)
            shouldThrow<IllegalArgumentException> { builder.value("OK") { index(0) } }
        }

        "index 0 cannot be reserved once a value declared it" {
            val builder = EnumBuilder("E")
            builder.value("OK") { index(0) }
            shouldThrow<IllegalArgumentException> { builder.reserved(0) }
        }
    })
