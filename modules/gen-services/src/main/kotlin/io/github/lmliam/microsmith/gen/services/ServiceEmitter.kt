package io.github.lmliam.microsmith.gen.services

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface ServiceEmitter<T : ServiceExtension> {
    val type: KClass<T>

    suspend fun T.emit(service: Service, space: FileSpace): List<GeneratedFile>
}
