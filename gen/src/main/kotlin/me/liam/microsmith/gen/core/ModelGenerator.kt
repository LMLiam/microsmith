package me.liam.microsmith.gen.core

import com.github.eventhorizonlab.spi.ServiceContract
import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import kotlin.reflect.KClass

@ServiceContract
interface ModelGenerator<T : MicrosmithExtension> {
    val extension: KClass<T>

    suspend fun T.generate(space: FileSpace): List<GeneratedFile>
}