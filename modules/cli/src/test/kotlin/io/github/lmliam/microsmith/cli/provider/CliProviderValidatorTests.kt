package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.gen.core.ModelGenerator
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.gen.schemas.SchemaEmitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.io.path.Path
import kotlin.reflect.KClass

class CliProviderValidatorTests :
    StringSpec({
        "reports missing built-in providers when none are present" {
            val errors = verifyBuiltinProviders(modelGenerators = emptyList(), schemaEmitters = emptyList())

            errors.shouldHaveSize(2)
            errors.shouldContain("Missing built-in ModelGenerator for SchemasExtension. Check CLI runtime packaging.")
            errors.shouldContain("Missing built-in SchemaEmitter for ProtobufSchema. Check CLI runtime packaging.")
        }

        "returns no errors when required providers are present" {
            val errors =
                verifyBuiltinProviders(
                    modelGenerators = listOf(SchemasGeneratorStub()),
                    schemaEmitters = listOf(ProtobufEmitterStub()),
                )

            errors shouldBe emptyList()
        }
    })

private class SchemasGeneratorStub : ModelGenerator<SchemasExtension> {
    override val extension: KClass<SchemasExtension> = SchemasExtension::class

    override suspend fun SchemasExtension.generate(space: FileSpace): List<GeneratedFile> = emptyList()
}

private class ProtobufEmitterStub : SchemaEmitter<ProtobufSchema> {
    override val type: KClass<ProtobufSchema> = ProtobufSchema::class

    override suspend fun ProtobufSchema.emit(space: FileSpace): GeneratedFile = GeneratedFile(
        relativePath = Path("proto/fake.proto"),
        contents = byteArrayOf(),
    )
}
