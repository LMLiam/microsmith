package me.liam.microsmith.dsl.schemas.protobuf.support

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
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
            val targetMsg = ProtobufSchema("package.Other", Message("Other"))
            val ref = Reference("package.Other")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufSchema("Foo", Message("Foo", fields = listOf(field)))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root = resolved.firstNotNullOf { schema -> (schema.schema as? Message)?.takeIf { it.name == "Foo" } }
            (root.fields[0] as ReferenceField).reference.type shouldBe targetMsg.schema
        }

        "resolves reference to enum" {
            val targetEnum = ProtobufSchema("package.Enum", Enum("Enum", emptyList()))
            val ref = Reference("package.Enum")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufSchema("Foo", Message("Foo", fields = listOf(field)))

            val resolved = resolveReferences(setOf(schema, targetEnum))
            val root = resolved.firstNotNullOf { schema -> (schema.schema as? Message)?.takeIf { it.name == "Foo" } }
            (root.fields[0] as ReferenceField).reference.type shouldBe targetEnum.schema
        }

        "resolves map value reference" {
            val targetMsg = ProtobufSchema("Other", Message("Other", emptyList(), emptyList()))
            val ref = Reference("Other")
            val mapField = MapField("m", 1, MapType(PrimitiveType.INT32, ref))
            val schema = ProtobufSchema("Root", Message("Root", listOf(mapField), emptyList()))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root = resolved.first { it.name == "Root" }.schema as Message
            ((root.fields[0] as MapField).type.value as Reference).type shouldBe targetMsg.schema
        }

        "resolves oneof reference" {
            val targetMsg = ProtobufSchema("Other", Message("Other", emptyList(), emptyList()))
            val ref = Reference("Other")
            val oneofField = OneofField("o", 1, ref)
            val oneof = Oneof("oneof", listOf(oneofField))
            val schema = ProtobufSchema("Root", Message("Root", emptyList(), listOf(oneof)))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root = resolved.first { it.name == "Root" }.schema as Message
            (
                (
                    root.oneofs[0]
                        .fields[0]
                        .fieldType as Reference
                ).type as Message
            ) shouldBe targetMsg.schema
        }

        "throws when reference cannot be resolved" {
            val ref = Reference("Other")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufSchema("Foo", Message("Foo", fields = listOf(field)))

            shouldThrow<IllegalStateException> {
                resolveReferences(setOf(schema))
            }
        }
    })