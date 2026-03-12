package me.liam.microsmith.gen.schemas

import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile

internal class SchemasGenerationService(
    private val emitterRegistry: SchemaEmitterRegistry = SchemaEmitterRegistry(),
) {
    suspend fun generate(extension: SchemasExtension, space: FileSpace): List<GeneratedFile> =
        extension.schemas.map { schema ->
            emitterRegistry.resolve(schema).run { schema.emit(space) }
        }
}
