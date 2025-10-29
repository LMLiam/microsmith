package me.liam.microsmith.dsl.schemas.protobuf.support

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufEnumSchema
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufMessageSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.*
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

class ReferenceResolverTests :
    StringSpec({
        "unqualified name appends to current segments" {
            getReferencePath(listOf("pkg", "sub"), "Foo") shouldBe listOf("pkg", "sub", "Foo")
        }

        "qualified name with dot ignores current segments" {
            getReferencePath(listOf("pkg", "sub"), "apkg.Foo") shouldBe listOf("apkg", "Foo")
        }

        "relative with one dot goes up one segment" {
            getReferencePath(listOf("pkg", "sub"), ".Foo") shouldBe listOf("pkg", "Foo")
        }

        "relative with more dots than segments drops all" {
            getReferencePath(listOf("pkg", "sub"), "....Foo") shouldBe listOf("Foo")
        }

        "relative with nested path works correctly" {
            getReferencePath(listOf("a", "b", "c"), "..x.Y") shouldBe listOf("a", "x", "Y")
        }

        "resolves reference to another message" {
            val targetMsg = ProtobufMessageSchema("package.Other", Message("Other"))
            val ref = Reference("package.Other")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufMessageSchema("Foo", Message("Foo", fields = listOf(field)))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root = resolved.filterIsInstance<ProtobufMessageSchema>().first { it.message.name == "Foo" }
            (root.message.fields[0] as ReferenceField).reference.type shouldBe targetMsg.message
        }

        "resolves reference to enum" {
            val targetEnum = ProtobufEnumSchema("package.Enum", Enum("Enum", emptyList()))
            val ref = Reference("package.Enum")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufMessageSchema("Foo", Message("Foo", fields = listOf(field)))

            val resolved = resolveReferences(setOf(schema, targetEnum))
            val root = resolved.filterIsInstance<ProtobufMessageSchema>().first { it.message.name == "Foo" }
            (root.message.fields[0] as ReferenceField).reference.type shouldBe targetEnum.enum
        }

        "resolves map value reference" {
            val targetMsg = ProtobufMessageSchema("Other", Message("Other", emptyList(), emptyList()))
            val ref = Reference("Other")
            val mapField = MapField("m", 1, MapType(PrimitiveType.INT32, ref))
            val schema = ProtobufMessageSchema("Root", Message("Root", listOf(mapField), emptyList()))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root = resolved.first { it.name == "Root" } as ProtobufMessageSchema
            ((root.message.fields[0] as MapField).type.value as Reference).type shouldBe targetMsg.message
        }

        "resolves oneof reference" {
            val targetMsg = ProtobufMessageSchema("Other", Message("Other", emptyList(), emptyList()))
            val ref = Reference("Other")
            val oneofField = OneofField("o", 1, ref)
            val oneof = Oneof("oneof", listOf(oneofField))
            val schema = ProtobufMessageSchema("Root", Message("Root", emptyList(), listOf(oneof)))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root = resolved.first { it.name == "Root" } as ProtobufMessageSchema
            (
                (
                    root.message.oneofs[0]
                        .fields[0]
                        .fieldType as Reference
                ).type as Message
            ) shouldBe targetMsg.message
        }

        "throws when reference cannot be resolved" {
            val ref = Reference("Other")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufMessageSchema("Foo", Message("Foo", fields = listOf(field)))

            shouldThrow<IllegalStateException> {
                resolveReferences(setOf(schema))
            }
        }
    })