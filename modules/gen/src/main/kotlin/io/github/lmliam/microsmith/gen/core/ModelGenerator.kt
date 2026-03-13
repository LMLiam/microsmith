package io.github.lmliam.microsmith.gen.core

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface ModelGenerator<T : MicrosmithExtension> {
    val extension: KClass<T>

    suspend fun T.generate(space: FileSpace): List<GeneratedFile>
}
