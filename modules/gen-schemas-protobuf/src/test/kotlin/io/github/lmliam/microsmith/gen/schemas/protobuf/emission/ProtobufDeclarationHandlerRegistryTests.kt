package io.github.lmliam.microsmith.gen.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

class ProtobufDeclarationHandlerRegistryTests :
    StringSpec({
        "resolves built-in protobuf declaration support" {
            val registry = ProtobufDeclarationHandlerRegistry()

            registry.resolve(Message("Test")).type shouldBe Message::class
        }

        "rejects duplicate declaration support for the same type" {
            shouldThrow<IllegalArgumentException> {
                ProtobufDeclarationHandlerRegistry(
                    listOf(
                        TestDeclarationHandler,
                        DuplicateTestDeclarationHandler,
                    ),
                )
            }
        }
    })

private data class TestType(
    override val name: String = "Test",
) : Type

private object TestDeclarationHandler : ProtobufDeclarationHandler<TestType> {
    override val type: KClass<TestType> = TestType::class

    override fun validate(schema: ProtobufSchema, qualifiedName: QualifiedSchemaName) = Unit

    override fun render(declaration: TestType): String = declaration.name
}

private object DuplicateTestDeclarationHandler : ProtobufDeclarationHandler<TestType> {
    override val type: KClass<TestType> = TestType::class

    override fun validate(schema: ProtobufSchema, qualifiedName: QualifiedSchemaName) = Unit

    override fun render(declaration: TestType): String = declaration.name
}
