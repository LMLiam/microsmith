package me.liam.microsmith.dsl.core

import kotlin.reflect.KClass

/**
 * Immutable snapshot produced by the DSL.
 *
 * - End-users never construct this directly; they use [microsmith] DSL entrypoint.
 * - Plugin authors consume this to read extensions they care about.
 */
class MicrosmithModel internal constructor(
    private val extensions: Map<KClass<out MicrosmithExtension>, MicrosmithExtension>
) {
    /**
     * Retrieve an extension of the given type, or null if not present.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : MicrosmithExtension> get(type: KClass<T>): T? {
        return extensions[type] as? T?
    }

    /**
     * Inline reified overload of [get] for convenience.
     */
    inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    /**
     * Internal: return a new model with the given extension attached.
     */
    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) = MicrosmithModel(extensions + (type to value))

    /**
     * Internal: reified overload of [with].
     */
    internal inline fun <reified T : MicrosmithExtension> with(value: T) = with(T::class, value)

    /**
     * Return the set of extension keys currently attached to this model.
     */
    fun keys() = extensions.keys

    companion object {
        /**
         * Create an empty model with no extensions.
         */
        fun empty() = MicrosmithModel(emptyMap())
    }
}