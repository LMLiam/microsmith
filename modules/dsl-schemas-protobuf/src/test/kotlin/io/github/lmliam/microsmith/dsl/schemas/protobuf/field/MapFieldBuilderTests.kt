package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MapFieldBuilderTests :
    StringSpec({
        "sets index correctly" {
            val builder = MapFieldBuilder()
            builder.index(42)
            builder.index shouldBe 42
        }

        "sets key once and throws on second set" {
            val builder = MapFieldBuilder()
            builder.key(PrimitiveType.STRING)
            builder.key shouldBe PrimitiveType.STRING

            shouldThrow<IllegalArgumentException> { builder.key(PrimitiveType.INT32) }
        }

        "sets value once and throws on second set" {
            val builder = MapFieldBuilder()
            builder.value(PrimitiveType.STRING)
            builder.value shouldBe PrimitiveType.STRING

            shouldThrow<IllegalArgumentException> { builder.value(PrimitiveType.INT32) }
        }

        "types sets both key and value" {
            val builder = MapFieldBuilder()
            builder.types(PrimitiveType.STRING to PrimitiveType.INT32)
            builder.key shouldBe PrimitiveType.STRING
            builder.value shouldBe PrimitiveType.INT32
        }

        "ref builds fully qualified name from segments" {
            val builder = MapFieldBuilder()
            builder.ref("Foo") shouldBe Reference.Local("Foo")
        }

        "ref with leading dot returns target one package back" {
            val builder = MapFieldBuilder()
            builder.ref(".Foo") shouldBe Reference.Relative(".Foo")
        }

        "ref with leading dot and multiple segments returns target one package back" {
            val builder = MapFieldBuilder()
            builder.ref(".Foo.Bar") shouldBe Reference.Relative(".Foo.Bar")
        }

        "ref with fully qualified name returns unchanged" {
            val builder = MapFieldBuilder()
            builder.ref("com.example.Foo") shouldBe Reference.Qualified("com.example.Foo")
        }

        "symbolic map values preserve their typed reference" {
            val builder = MapFieldBuilder()
            val target = MessageRef("pkg.sub.User")

            builder.value(target)
            builder.value shouldBe Reference.Symbolic(target)
        }
    })
