package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.gen.core.ModelGenerator
import io.github.lmliam.microsmith.gen.schemas.SchemaEmitter
import java.util.ServiceLoader

internal fun verifyBuiltinProviders(
    modelGenerators: List<ModelGenerator<*>> = loadModelGenerators(),
    schemaEmitters: List<SchemaEmitter<*>> = loadSchemaEmitters(),
): List<String> {
    val errors = mutableListOf<String>()

    if (modelGenerators.none { it.extension == SchemasExtension::class }) {
        errors += "Missing built-in ModelGenerator for SchemasExtension. Check CLI runtime packaging."
    }

    if (schemaEmitters.none { it.type == ProtobufSchema::class }) {
        errors += "Missing built-in SchemaEmitter for ProtobufSchema. Check CLI runtime packaging."
    }

    return errors
}

private fun loadModelGenerators() = ServiceLoader.load(ModelGenerator::class.java).iterator().asSequence().toList()

private fun loadSchemaEmitters() = ServiceLoader.load(SchemaEmitter::class.java).iterator().asSequence().toList()
