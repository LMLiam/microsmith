package me.liam.microsmith.gen.schemas

import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

class SchemasGenerator(
    private val emitters: Map<KClass<out Schema>, SchemaEmitter<out Schema>>
) : ModelGenerator<SchemasExtension> {
    override suspend fun SchemasExtension.generate(space: FileSpace): List<GeneratedFile> = schemas
        .map { schema ->
            val emitter = emitters[schema::class] ?: error("No emitter found for schema type: ${schema::class}")
            @Suppress("UNCHECKED_CAST")
            (emitter as SchemaEmitter<Schema>).run { schema.emit(space) }
        }
}