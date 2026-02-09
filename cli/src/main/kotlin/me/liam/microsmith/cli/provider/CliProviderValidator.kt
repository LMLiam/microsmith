package me.liam.microsmith.cli.provider

import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.schemas.SchemaEmitter
import java.util.ServiceLoader

internal fun verifyBuiltinProviders(
    modelGenerators: List<ModelGenerator<*>> = loadModelGenerators(),
    schemaEmitters: List<SchemaEmitter<*>> = loadSchemaEmitters()
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
