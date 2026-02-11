package me.liam.microsmith.dsl.schemas.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private object FakeSchemaType : SchemaType {
    override val typeName = "fake"
}

private data class FakeSchema(
    override val type: SchemaType = FakeSchemaType,
    override val name: String,
) : Schema

class SchemasBuilderTests :
    StringSpec({
        "register adds schema to builder" {
            val builder = SchemasBuilder()
            val schema = FakeSchema(name = "User")

            builder.register(schema)

            builder.schemas shouldContainExactly listOf(schema)
        }

        "register throws if schema is blank" {
            val builder = SchemasBuilder()
            val schema = FakeSchema(name = "")

            shouldThrow<IllegalArgumentException> {
                builder.register(schema)
            }
        }

        "register throws for duplicate schema type and name" {
            val builder = SchemasBuilder()
            val schema = FakeSchema(name = "User")

            builder.register(schema)

            shouldThrow<IllegalArgumentException> {
                builder.register(FakeSchema(name = "User"))
            }
        }

        "build produces SchemasExtension with all schemas" {
            val builder = SchemasBuilder()
            val s1 = FakeSchema(name = "User")
            val s2 = FakeSchema(name = "Company")

            builder.register(s1)
            builder.register(s2)

            val ext = builder.toExtension()

            ext.schemas shouldContainExactly setOf(s1, s2)
        }

        "SchemasExtension is immutable snapshot" {
            val builder = SchemasBuilder()
            val schema = FakeSchema(name = "User")
            builder.register(schema)

            val ext = builder.toExtension()

            builder.register(FakeSchema(name = "Company"))

            ext.schemas shouldContainExactly setOf(schema)
        }
    })
