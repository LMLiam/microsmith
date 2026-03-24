package io.github.lmliam.microsmith.gen.services

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.core.ModelGenerator
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile

@ServiceProvider(ModelGenerator::class)
class ServicesGenerator : ModelGenerator<ServicesExtension> {
    private val generationService: ServicesGenerationService

    constructor() {
        generationService = ServicesGenerationService()
    }

    internal constructor(generationService: ServicesGenerationService) {
        this.generationService = generationService
    }

    override val extension get() = ServicesExtension::class

    override suspend fun ServicesExtension.generate(space: FileSpace): List<GeneratedFile> =
        generationService.generate(this, space)
}
