package io.github.lmliam.microsmith.gen.schemas

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.gen.core.ModelGenerator
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile

@ServiceProvider(ModelGenerator::class)
class SchemasGenerator : ModelGenerator<SchemasExtension> {
    private val generationService: SchemasGenerationService

    constructor() {
        generationService = SchemasGenerationService()
    }

    internal constructor(generationService: SchemasGenerationService) {
        this.generationService = generationService
    }

    override val extension get() = SchemasExtension::class

    override suspend fun SchemasExtension.generate(space: FileSpace): List<GeneratedFile> =
        generationService.generate(this, space)
}
