package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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

        "reference path rejects empty trailing segments" {
            shouldThrow<IllegalArgumentException> {
                getReferencePath(listOf("pkg"), ".")
            }
        }

        "resolves reference to another message" {
            val targetMsg = ProtobufSchema("package.Other", Message("Other"))
            val ref = Reference("package.Other")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufSchema("Foo", Message("Foo", fields = listOf(field)))

            val resolved = resolveReferences(setOf(schema, targetMsg))
            val root =
                resolved.firstNotNullOf { resolvedSchema ->
                    (resolvedSchema.schema as? Message)?.takeIf { it.name == "Foo" }
                }
            (root.fields[0] as ReferenceField).reference.type shouldBe targetMsg.schema
        }

        "resolves reference to enum" {
            val targetEnum = ProtobufSchema("package.Enum", Enum("Enum", emptyList()))
            val ref = Reference("package.Enum")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufSchema("Foo", Message("Foo", fields = listOf(field)))

            val resolved = resolveReferences(setOf(schema, targetEnum))
            val root =
                resolved.firstNotNullOf { resolvedSchema ->
                    (resolvedSchema.schema as? Message)?.takeIf { it.name == "Foo" }
                }
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
            val resolvedReferenceType =
                (root.oneofs[0].fields[0].fieldType as Reference).type
                    ?: error("Expected oneof field reference to resolve to a concrete schema type.")
            resolvedReferenceType shouldBe targetMsg.schema
        }

        "throws when reference cannot be resolved with context" {
            val ref = Reference("Other")
            val field = ReferenceField("f", 1, ref)
            val schema = ProtobufSchema("Foo", Message("Foo", fields = listOf(field)))

            val error = shouldThrow<IllegalStateException> {
                resolveReferences(setOf(schema))
            }

            error.message shouldBe """
                Unresolved references:
                - Unresolved reference 'Other' in message Foo field 'f'
            """.trimIndent()
        }
    })
