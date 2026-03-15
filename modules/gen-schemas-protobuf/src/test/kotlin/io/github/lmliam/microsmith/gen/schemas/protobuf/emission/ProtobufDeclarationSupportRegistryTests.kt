package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

class ProtobufDeclarationSupportRegistryTests :
    StringSpec({
        "resolves built-in protobuf declaration support" {
            val registry = ProtobufDeclarationSupportRegistry()

            registry.resolve(Message("Test")).type shouldBe Message::class
        }

        "rejects duplicate declaration support for the same type" {
            shouldThrow<IllegalArgumentException> {
                ProtobufDeclarationSupportRegistry(
                    listOf(
                        TestDeclarationSupport,
                        DuplicateTestDeclarationSupport,
                    ),
                )
            }
        }
    })

private data class TestType(
    override val name: String = "Test",
) : io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

private object TestDeclarationSupport : ProtobufDeclarationSupport<TestType> {
    override val type: KClass<TestType> = TestType::class

    override fun validate(
        schema: io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema,
        qualifiedName: io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName,
    ) = Unit

    override fun render(declaration: TestType): String = declaration.name
}

private object DuplicateTestDeclarationSupport : ProtobufDeclarationSupport<TestType> {
    override val type: KClass<TestType> = TestType::class

    override fun validate(
        schema: io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema,
        qualifiedName: io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName,
    ) = Unit

    override fun render(declaration: TestType): String = declaration.name
}
