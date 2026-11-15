package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.merge.mergeModelExtension
import kotlin.reflect.KClass

/** Immutable snapshot of the service-scoped extension payloads attached to a service. */
public class ServiceModel
internal constructor(private val extensions: Map<KClass<out ServiceExtension>, ServiceExtension>) {
    @Suppress("UNCHECKED_CAST") public fun <T : ServiceExtension> get(type: KClass<T>): T? = extensions[type] as? T?

    public inline fun <reified T : ServiceExtension> get(): T? = get(T::class)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ServiceExtension> with(type: KClass<T>, value: T) =
        ServiceModel(extensions + (mapOf(type to mergeModelExtension(extensions[type] as T?, value))))

    internal fun merge(other: ServiceModel): ServiceModel =
        ServiceModel(
            extensions + other.extensions.mapValues { (type, value) -> mergeModelExtension(extensions[type], value) },
        )

    internal fun keys(): Set<KClass<out ServiceExtension>> = extensions.keys

    override fun equals(other: Any?): Boolean = other is ServiceModel && extensions == other.extensions

    override fun hashCode(): Int = extensions.hashCode()

    internal companion object {
        internal fun empty() = ServiceModel(emptyMap())
    }
}
