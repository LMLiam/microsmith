package io.github.lmliam.microsmith.dsl.services.core

import kotlin.reflect.KClass

/**
 * Immutable snapshot of the service-scoped extension payloads attached to a service.
 */
class ServiceModel internal constructor(
    private val extensions: Map<KClass<out ServiceExtension>, ServiceExtension>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : ServiceExtension> get(type: KClass<T>) = extensions[type] as? T?

    inline fun <reified T : ServiceExtension> get(): T? = get(T::class)

    internal fun <T : ServiceExtension> with(type: KClass<T>, value: T) = ServiceModel(
        extensions + (mapOf(type to value)),
    )

    fun keys() = extensions.keys

    override fun equals(other: Any?): Boolean {
        return other is ServiceModel && extensions == other.extensions
    }

    override fun hashCode(): Int = extensions.hashCode()

    companion object {
        fun empty() = ServiceModel(emptyMap())
    }
}
