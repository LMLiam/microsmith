package me.liam.microsmith.gen.core

import me.liam.microsmith.dsl.core.MicrosmithExtension
import java.util.ServiceLoader
import kotlin.reflect.KClass

object GeneratorRegistry {
    @PublishedApi
    internal val generators = mutableMapOf<KClass<out MicrosmithExtension>, ModelGenerator<*>>()

    fun <T : MicrosmithExtension> ModelGenerator<T>.register() {
        generators[this.extension] = this
    }

    fun load() {
        ServiceLoader.load(ModelGenerator::class.java).forEach { it.register() }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> T.getGenerator() = generators[this::class] as? ModelGenerator<T>
}