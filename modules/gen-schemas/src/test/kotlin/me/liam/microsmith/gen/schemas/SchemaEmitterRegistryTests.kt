package me.liam.microsmith.gen.schemas

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemaType
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import kotlin.io.path.Path
import kotlin.reflect.KClass

class SchemaEmitterRegistryTests :
    StringSpec({
        "resolve returns emitter for matching schema class" {
            val emitter = TestSchemaEmitter()
            val registry = SchemaEmitterRegistry(listOf(emitter))

            registry.resolve(TestSchema()).type shouldBe TestSchema::class
        }

        "resolve rejects duplicate emitters for the same schema class" {
            shouldThrow<IllegalArgumentException> {
                SchemaEmitterRegistry(listOf(TestSchemaEmitter(), DuplicateTestSchemaEmitter()))
            }
        }

        "resolve rejects missing emitter" {
            val registry = SchemaEmitterRegistry(emptyList())

            shouldThrow<IllegalStateException> {
                registry.resolve(TestSchema())
            }
        }
    })

private class TestSchemaEmitter : SchemaEmitter<TestSchema> {
    override val type: KClass<TestSchema> = TestSchema::class

    override suspend fun TestSchema.emit(space: FileSpace): GeneratedFile =
        GeneratedFile(Path("test.out"), byteArrayOf())
}

private class DuplicateTestSchemaEmitter : SchemaEmitter<TestSchema> {
    override val type: KClass<TestSchema> = TestSchema::class

    override suspend fun TestSchema.emit(space: FileSpace): GeneratedFile =
        GeneratedFile(Path("duplicate.out"), byteArrayOf())
}

private data class TestSchema(
    override val type: SchemaType = TestSchemaType,
    override val name: String = "test",
) : Schema

private data object TestSchemaType : SchemaType {
    override val typeName: String = "test"
}
