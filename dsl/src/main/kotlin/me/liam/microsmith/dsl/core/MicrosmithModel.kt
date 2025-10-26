package me.liam.microsmith.dsl

import kotlin.reflect.KClass

class MicrosmithModel internal constructor(
    private val extensions: Map<KClass<out RootExtension>, RootExtension>
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : RootExtension> get(type: KClass<T>): T? {
        return extensions[type] as? T?
    }

    inline fun <reified T : RootExtension> get(): T? = get(T::class)

    internal fun <T : RootExtension> with(type: KClass<T>, value: T) = MicrosmithModel(extensions + (type to value))

    internal inline fun <reified T : RootExtension> with(value: T) = with(T::class, value)

    companion object {
        fun empty() = MicrosmithModel(emptyMap())
    }
}