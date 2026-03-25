package io.github.lmliam.microsmith.gen.services

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.core.ModelExtension
import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface ServiceEmitter<T : ModelExtension> {
    val type: KClass<T>

    /**
     * Emits files for a services-level extension attached under `services { ... }`.
     */
    suspend fun T.emit(services: ServicesExtension, space: FileSpace): List<GeneratedFile> = emptyList()

    /**
     * Emits files for a service-level extension attached under `"ServiceName" { ... }`.
     */
    suspend fun T.emit(service: Service, services: ServicesExtension, space: FileSpace): List<GeneratedFile> =
        emptyList()
}
