package io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.helpers.mergeModelExtension
import kotlin.reflect.KClass

/**
 * Immutable snapshot of shared .NET-specific extensions.
 */
class DotnetDefaultsModel internal constructor(
    private val extensions: Map<KClass<out MicrosmithExtension>, MicrosmithExtension>,
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> get(type: KClass<T>) = extensions[type] as? T?

    inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    @Suppress("UNCHECKED_CAST")
    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) = DotnetDefaultsModel(
        extensions + (mapOf(type to mergeModelExtension(extensions[type] as T?, value))),
    )

    internal fun merge(other: DotnetDefaultsModel): DotnetDefaultsModel = DotnetDefaultsModel(
        extensions +
            other.extensions.mapValues { (type, value) ->
                mergeModelExtension(extensions[type] as MicrosmithExtension?, value)
            },
    )

    fun keys() = extensions.keys

    override fun equals(other: Any?): Boolean = other is DotnetDefaultsModel && extensions == other.extensions

    override fun hashCode(): Int = extensions.hashCode()

    companion object {
        fun empty() = DotnetDefaultsModel(emptyMap())
    }
}
