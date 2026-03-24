package io.github.lmliam.microsmith.dsl.services.dotnet.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.helpers.mergeModelExtension
import kotlin.reflect.KClass

/**
 * Immutable snapshot of shared .NET-specific extensions.
 */
class DotnetSharedModel internal constructor(
    private val extensions: Map<KClass<out MicrosmithExtension>, MicrosmithExtension>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> get(type: KClass<T>) = extensions[type] as? T?

    inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) = DotnetSharedModel(
        extensions + (mapOf(type to mergeModelExtension(extensions[type] as T?, value))),
    )

    internal fun merge(other: DotnetSharedModel): DotnetSharedModel = DotnetSharedModel(
        extensions +
            other.extensions.mapValues { (type, value) ->
                mergeModelExtension(extensions[type] as MicrosmithExtension?, value)
            },
    )

    fun keys() = extensions.keys

    override fun equals(other: Any?): Boolean {
        return other is DotnetSharedModel && extensions == other.extensions
    }

    override fun hashCode(): Int = extensions.hashCode()

    companion object {
        fun empty() = DotnetSharedModel(emptyMap())
    }
}
