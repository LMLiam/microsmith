package io.github.lmliam.microsmith.gen.services

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface SharedServiceEmitter<T : MicrosmithExtension> {
    val type: KClass<T>

    suspend fun T.emit(services: ServicesExtension, space: FileSpace): List<GeneratedFile>
}
