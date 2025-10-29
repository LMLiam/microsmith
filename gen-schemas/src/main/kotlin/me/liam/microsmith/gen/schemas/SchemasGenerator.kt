package me.liam.microsmith.gen.schemas

import com.github.eventhorizonlab.spi.ServiceProvider
import kotlinx.coroutines.async
import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceProvider(ModelGenerator::class)
class SchemasGenerator(
    private val emitters: Set<SchemaEmitter<out Schema>>
) : ModelGenerator<SchemasExtension> {
    override val extension get() = SchemasExtension::class

    override suspend fun SchemasExtension.generate(space: FileSpace) =
        schemas.map { schema ->
            val emitter =
                emitters.firstOrNull { it.type == schema::class }
                    ?: error("No emitter found for schema type: ${schema::class}")
            @Suppress("UNCHECKED_CAST")
            (emitter as SchemaEmitter<Schema>).run { schema.emit(space) }
        }
}