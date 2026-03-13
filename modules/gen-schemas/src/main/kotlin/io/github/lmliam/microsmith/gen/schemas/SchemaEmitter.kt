package io.github.lmliam.microsmith.gen.schemas

import com.github.eventhorizonlab.spi.ServiceContract
import io.github.lmliam.microsmith.dsl.schemas.core.Schema
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface SchemaEmitter<T : Schema> {
    val type: KClass<T>

    suspend fun T.emit(space: FileSpace): GeneratedFile
}
