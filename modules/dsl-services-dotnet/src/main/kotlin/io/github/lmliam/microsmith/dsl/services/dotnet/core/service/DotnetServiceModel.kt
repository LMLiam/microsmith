package io.github.lmliam.microsmith.dsl.services.dotnet.core.service

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import kotlin.reflect.KClass

/**
 * Immutable snapshot of service-scoped .NET-specific extensions.
 */
class DotnetServiceModel internal constructor(
    private val extensions: Map<KClass<out ServiceExtension>, ServiceExtension>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : ServiceExtension> get(type: KClass<T>) = extensions[type] as? T?

    inline fun <reified T : ServiceExtension> get(): T? = get(T::class)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ServiceExtension> with(type: KClass<T>, value: T) = DotnetServiceModel(
        extensions + (mapOf(type to mergeServiceExtension(extensions[type] as T?, value))),
    )

    internal fun merge(other: DotnetServiceModel): DotnetServiceModel = DotnetServiceModel(
        extensions +
            other.extensions.mapValues { (type, value) ->
                mergeServiceExtension(extensions[type] as ServiceExtension?, value)
            },
    )

    fun keys() = extensions.keys

    override fun equals(other: Any?): Boolean {
        return other is DotnetServiceModel && extensions == other.extensions
    }

    override fun hashCode(): Int = extensions.hashCode()

    companion object {
        fun empty() = DotnetServiceModel(emptyMap())
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : ServiceExtension> mergeServiceExtension(existing: T?, incoming: T): T = when {
    existing == null -> incoming
    existing::class == incoming::class && existing is MergeableExtension<*> ->
        (existing as MergeableExtension<T>).merge(incoming)
    else -> incoming
}
