package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.merge.mergeModelExtension
import kotlin.reflect.KClass

/** Immutable snapshot of the shared service-scoped extension payloads attached to `services { ... }`. */
public class ServicesModel
internal constructor(private val extensions: Map<KClass<out MicrosmithExtension>, MicrosmithExtension>) {
    @Suppress("UNCHECKED_CAST") public fun <T : MicrosmithExtension> get(type: KClass<T>): T? = extensions[type] as? T?

    public inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) =
        ServicesModel(extensions + (mapOf(type to mergeModelExtension(extensions[type] as T?, value))))

    internal fun merge(other: ServicesModel): ServicesModel =
        ServicesModel(
            extensions + other.extensions.mapValues { (type, value) -> mergeModelExtension(extensions[type], value) },
        )

    internal fun keys(): Set<KClass<out MicrosmithExtension>> = extensions.keys

    override fun equals(other: Any?): Boolean = other is ServicesModel && extensions == other.extensions

    override fun hashCode(): Int = extensions.hashCode()

    internal companion object {
        internal fun empty() = ServicesModel(emptyMap())
    }
}
