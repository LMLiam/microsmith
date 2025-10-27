package me.liam.microsmith.dsl.schemas.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

private data class FakeSchema(
    override val type: SchemaType = object : SchemaType { override val typeName = "fake" },
    override val name: String
) : Schema

class SchemasBuilderTests : StringSpec({
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

    "build produces SchemasExtension with all schemas" {
        val builder = SchemasBuilder()
        val s1 = FakeSchema(name = "User")
        val s2 = FakeSchema(name = "Company")

        builder.register(s1)
        builder.register(s2)

        val ext = builder.build()

        ext.schemas shouldContainExactly listOf(s1, s2)
    }

    "SchemasExtension is immutable snapshot" {
        val builder = SchemasBuilder()
        val schema = FakeSchema(name = "User")
        builder.register(schema)

        val ext = builder.build()

        builder.register(FakeSchema(name = "Company"))

        ext.schemas shouldContainExactly listOf(schema)
    }
})