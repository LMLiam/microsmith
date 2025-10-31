package me.liam.microsmith.dsl.schemas.protobuf.field

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MapFieldBuilderTests :
    StringSpec({
        "sets index correctly" {
            val builder = MapFieldBuilder(listOf("me", "liam"))
            builder.index(42)
            builder.index shouldBe 42
        }

        "sets key once and throws on second set" {
            val builder = MapFieldBuilder(emptyList())
            builder.key(PrimitiveType.STRING)
            builder.key shouldBe PrimitiveType.STRING

            shouldThrow<IllegalArgumentException> {
                builder.key(PrimitiveType.INT32)
            }
        }

        "sets value once and throws on second set" {
            val builder = MapFieldBuilder(emptyList())
            builder.value(PrimitiveType.STRING)
            builder.value shouldBe PrimitiveType.STRING

            shouldThrow<IllegalArgumentException> {
                builder.value(PrimitiveType.INT32)
            }
        }

        "types sets both key and value" {
            val builder = MapFieldBuilder(emptyList())
            builder.types(PrimitiveType.STRING to PrimitiveType.INT32)
            builder.key shouldBe PrimitiveType.STRING
            builder.value shouldBe PrimitiveType.INT32
        }

        "ref builds fully qualified name from segments" {
            val builder = MapFieldBuilder(listOf("me", "liam"))
            val ref = builder.ref("Foo")
            ref.name shouldBe "me.liam.Foo"
        }

        "ref with empty segments returns just target" {
            val builder = MapFieldBuilder(emptyList())
            val ref = builder.ref("Foo")
            ref.name shouldBe "Foo"
        }

        "ref with leading dot returns target one package back" {
            val builder = MapFieldBuilder(listOf("me", "liam"))
            val ref = builder.ref(".Foo")
            ref.name shouldBe "me.Foo"
        }

        "ref with leading dot and multiple segments returns target one package back" {
            val builder = MapFieldBuilder(listOf("me", "liam"))
            val ref = builder.ref(".Foo.Bar")
            ref.name shouldBe "me.Foo.Bar"
        }

        "ref with fully qualified name returns unchanged" {
            val builder = MapFieldBuilder(listOf("me", "liam"))
            val ref = builder.ref("com.example.Foo")
            ref.name shouldBe "com.example.Foo"
        }
    })