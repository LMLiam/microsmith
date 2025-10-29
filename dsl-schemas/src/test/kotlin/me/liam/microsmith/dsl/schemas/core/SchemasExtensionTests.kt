package me.liam.microsmith.dsl.schemas.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private enum class TestSchemaTypes(
    override val typeName: String
) : SchemaType {
    PROTOBUF("protobuf"),
    JSON("json")
}

private data class ExtFakeSchema(
    override val type: SchemaType,
    override val name: String
) : Schema

class SchemasExtensionTests :
    StringSpec({
        "find returns schema when present" {
            val s1 = ExtFakeSchema(TestSchemaTypes.PROTOBUF, "User")
            val ext = SchemasExtension(setOf(s1))

            ext.find(TestSchemaTypes.PROTOBUF, "User") shouldBe s1
        }

        "find returns null when schema not present" {
            val s1 = ExtFakeSchema(TestSchemaTypes.PROTOBUF, "User")
            val ext = SchemasExtension(setOf(s1))

            ext.find(TestSchemaTypes.JSON, "User") shouldBe null
        }

        "require returns schema when present" {
            val s1 = ExtFakeSchema(TestSchemaTypes.PROTOBUF, "User")
            val ext = SchemasExtension(setOf(s1))

            ext.require(TestSchemaTypes.PROTOBUF, "User") shouldBe s1
        }

        "require throws when schema not present" {
            val s1 = ExtFakeSchema(TestSchemaTypes.PROTOBUF, "User")
            val ext = SchemasExtension(setOf(s1))

            shouldThrow<IllegalStateException> {
                ext.require(TestSchemaTypes.JSON, "User")
            }
        }

        "allOf returns all schemas of given type" {
            val s1 = ExtFakeSchema(TestSchemaTypes.PROTOBUF, "User")
            val s2 = ExtFakeSchema(TestSchemaTypes.PROTOBUF, "Company")
            val s3 = ExtFakeSchema(TestSchemaTypes.JSON, "User")
            val ext = SchemasExtension(setOf(s1, s2, s3))

            ext.allOf(TestSchemaTypes.PROTOBUF) shouldBe setOf(s1, s2)
            ext.allOf(TestSchemaTypes.JSON) shouldBe setOf(s3)
        }
    })