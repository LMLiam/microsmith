package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.helpers.extensions
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile

internal class ServicesGenerationService(
    private val emitterRegistry: ServiceEmitterRegistry = ServiceEmitterRegistry(),
) {
    suspend fun generate(extension: ServicesExtension, space: FileSpace): List<GeneratedFile> =
        extension.services.flatMap { service ->
            service.model.extensions().flatMap { serviceExtension ->
                emitterRegistry.resolve(serviceExtension).run { serviceExtension.emit(service, space) }
            }
        }
}
