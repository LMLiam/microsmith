package me.liam.microsmith.gen.schemas

import com.github.eventhorizonlab.spi.ServiceProvider
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile

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
