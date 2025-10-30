package me.liam.microsmith.dsl.schemas.protobuf

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

class ProtobufBuilderTests :
    StringSpec({
        "builds a simple message schema with fully qualified name" {
            val builder = ProtobufBuilder(segments = listOf("me", "liam"))
            builder.message("Person") {
                int32("id") { index(1) }
                string("name") { index(2) }
            }
            val schemas = builder.build()
            val msg = schemas.first { it.schema is Message }
            msg.name shouldBe "me.liam.Person"
            msg.schema.name shouldBe "Person"
            (msg.schema as Message).fields.map { it.name } shouldContainExactly listOf("id", "name")
        }

        "builds a simple enum schema with fully qualified name" {
            val builder = ProtobufBuilder(segments = listOf("me", "liam"))
            builder.enum("Gender") {
                value("MALE")
                value("FEMALE")
            }
            val schemas = builder.build()
            val enum = schemas.first { it.schema is Enum }
            enum.name shouldBe "me.liam.Gender"
            enum.schema.name shouldBe "Gender"
            (enum.schema as Enum).values.map { it.name } shouldContainAll listOf("MALE", "FEMALE")
        }

        "nested package segments via String.invoke build qualified names" {
            val builder = ProtobufBuilder(segments = listOf("me", "liam"))
            builder.apply {
                "foo.bar" {
                    "baz" {
                        message("Baz") { int32("id") { index(1) } }
                        enum("Status") { value("OK") { index(1) } }
                    }
                }
            }
            val schemas = builder.build()
            val names = schemas.map { it.name }
            names shouldContainExactlyInAnyOrder listOf("me.liam.foo.bar.baz.Baz", "me.liam.foo.bar.baz.Status")
        }

        "version appends vN segment to namespace" {
            val builder = ProtobufBuilder(segments = listOf("me", "liam"))
            builder.version(2) {
                message("Thing") { int32("id") { index(1) } }
            }
            val schemas = builder.build()
            schemas.map { it.name } shouldContainExactlyInAnyOrder listOf("me.liam.v2.Thing")
        }

        "nested and top-level schemas coexist" {
            val builder = ProtobufBuilder()
            builder.message("Top") { int32("id") { index(1) } }
            builder.apply {
                "pkg" {
                    enum("E") { value("X") { index(1) } }
                }
            }
            val schemas = builder.build()
            schemas.map { it.name } shouldContainExactlyInAnyOrder listOf("Top", "pkg.E")
        }
    })