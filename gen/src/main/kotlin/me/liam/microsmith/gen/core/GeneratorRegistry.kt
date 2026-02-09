package me.liam.microsmith.gen.core

import me.liam.microsmith.dsl.core.MicrosmithExtension
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object GeneratorRegistry {
    @PublishedApi
    internal val generators = ConcurrentHashMap<KClass<out MicrosmithExtension>, ModelGenerator<*>>()
    private val loadLock = Any()
    @Volatile
    private var loaded = false

    fun <T : MicrosmithExtension> ModelGenerator<T>.register() {
        generators[this.extension] = this
    }

    fun load() {
        if (loaded) {
            return
        }
        synchronized(loadLock) {
            if (loaded) {
                return
            }
            ServiceLoader.load(ModelGenerator::class.java).forEach { it.register() }
            loaded = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> T.getGenerator() = generators[this::class] as? ModelGenerator<T>
}
