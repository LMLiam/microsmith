package me.liam.microsmith.dsl.schemas.protobuf.oneof

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference

class OneofBuilderTests :
    StringSpec({
        fun newBuilder(
            segments: List<String> = listOf("me, liam"),
            allocateIndex: (Int?) -> Int = { it ?: 1 },
            usedNames: MutableList<String> = mutableListOf()
        ): Pair<OneofBuilder, MutableList<String>> {
            val builder =
                OneofBuilder(
                    name = "TestOneof",
                    segments = segments,
                    allocateIndex = allocateIndex,
                    useName = { usedNames += it }
                )
            return builder to usedNames
        }

        "creates primitive field with allocated index" {
            val (builder, _) = newBuilder(allocateIndex = { it ?: 42 })
            val field = builder.int32("foo") { index(5) }
            field.name shouldBe "foo"
            field.index shouldBe 5
            field.fieldType shouldBe PrimitiveType.INT32
        }

        "creates reference field with fqName from segments" {
            val (builder, _) = newBuilder(segments = listOf("pkg", "sub"))
            val field = builder.ref("bar", "Target") { index(7) }
            field.name shouldBe "bar"
            field.index shouldBe 7
            field.fieldType.shouldBeInstanceOf<Reference>()
            field.fieldType.name shouldBe "pkg.sub.Target"
        }

        "ref with fully qualified target ignores segments" {
            val (builder, _) = newBuilder(segments = listOf("pkg", "sub"))
            val field = builder.ref("target", "pkg.else.Target") { index(7) }
            field.name shouldBe "target"
            field.index shouldBe 7
            field.fieldType.shouldBeInstanceOf<Reference>()
            field.fieldType.name shouldBe "pkg.else.Target"
        }

        "duplicate field names throw" {
            val (builder, _) = newBuilder()
            builder.int32("dup")
            shouldThrow<IllegalArgumentException> {
                builder.string("dup")
            }
        }

        "used names are tracked" {
            val (builder, usedNames) = newBuilder()
            builder.int32("foo")
            builder.string("bar")
            usedNames shouldBe listOf("foo", "bar")
        }

        "index is allocated" {
            var captured: Int? = null
            val (builder, _) =
                newBuilder(allocateIndex = { idx ->
                    captured = idx
                    idx ?: 99
                })
            builder.int64("num") { index(123) }
            captured shouldBe 123
        }

        "allocated index is called with null if no index set" {
            var captured: Int? = null
            val (builder, _) =
                newBuilder(allocateIndex = { idx ->
                    captured = idx
                    77
                })
            val field = builder.bool("flag")
            captured shouldBe null
            field.index shouldBe 77
        }

        "build sorts fields by index" {
            val (builder, _) = newBuilder()
            builder.int32("x") { index(20) }
            builder.int32("y") { index(10) }
            val oneof = builder.build()
            oneof.fields.map { it.name } shouldBe listOf("y", "x")
        }
    })