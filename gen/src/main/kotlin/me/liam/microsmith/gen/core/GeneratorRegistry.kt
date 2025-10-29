package me.liam.microsmith.gen.core

import me.liam.microsmith.dsl.core.MicrosmithExtension
import kotlin.reflect.KClass

object GeneratorRegistry {
    @PublishedApi
    internal val generators =
        mutableMapOf<KClass<out MicrosmithExtension>, ModelGenerator<*>>()

    inline fun <reified T : MicrosmithExtension> ModelGenerator<T>.register() {
        generators[T::class] = this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> T.getGenerator() = generators[this::class] as? ModelGenerator<T>
        ?: error("No generator registered for ${this::class.simpleName}")
}