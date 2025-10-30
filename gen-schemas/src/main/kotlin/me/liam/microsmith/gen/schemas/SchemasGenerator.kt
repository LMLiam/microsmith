package me.liam.microsmith.gen.schemas

import com.github.eventhorizonlab.spi.ServiceProvider
import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import java.util.*

@ServiceProvider(ModelGenerator::class)
class SchemasGenerator : ModelGenerator<SchemasExtension> {
    override val extension get() = SchemasExtension::class

    override suspend fun SchemasExtension.generate(space: FileSpace): List<GeneratedFile> {
        val emitters = ServiceLoader.load(SchemaEmitter::class.java)
        return schemas.map { schema ->
            val emitter =
                emitters.firstOrNull { it.type == schema::class }
                    ?: error("No emitter found for schema type: ${schema::class}")
            @Suppress("UNCHECKED_CAST")
            (emitter as SchemaEmitter<Schema>).run { schema.emit(space) }
        }
    }
}