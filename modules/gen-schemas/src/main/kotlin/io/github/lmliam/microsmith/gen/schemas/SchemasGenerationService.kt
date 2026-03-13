package io.github.lmliam.microsmith.gen.schemas

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile

internal class SchemasGenerationService(
    private val emitterRegistry: SchemaEmitterRegistry = SchemaEmitterRegistry(),
) {
    suspend fun generate(extension: SchemasExtension, space: FileSpace): List<GeneratedFile> =
        extension.schemas.map { schema ->
            emitterRegistry.resolve(schema).run { schema.emit(space) }
        }
}
