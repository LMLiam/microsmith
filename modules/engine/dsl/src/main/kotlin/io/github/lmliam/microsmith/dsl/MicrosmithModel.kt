package io.github.lmliam.microsmith.dsl

import io.github.lmliam.microsmith.dsl.merge.mergeModelExtension
import kotlin.reflect.KClass

/**
 * Immutable snapshot produced by the DSL.
 *
 * - End-users never construct this directly; they use [microsmith] DSL entrypoint.
 * - Plugin authors consume this to read extensions they care about.
 */
public class MicrosmithModel
internal constructor(private val extensions: Map<KClass<out MicrosmithExtension>, MicrosmithExtension>) {
    /**
     * Retrieve the extension registered for the given type.
     *
     * @return the extension, or `null` when the type is not present.
     */
    @Suppress("UNCHECKED_CAST") public fun <T : MicrosmithExtension> get(type: KClass<T>): T? = extensions[type] as? T

    /** Inline reified overload of [get] for convenience. */
    public inline fun <reified T : MicrosmithExtension> get(): T? = get(T::class)

    /** Internal: return a new model with the given extension attached. */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : MicrosmithExtension> with(type: KClass<T>, value: T) =
        MicrosmithModel(extensions + (mapOf(type to mergeModelExtension(extensions[type] as T?, value))))

    /** Internal: reified overload of [with]. */
    internal inline fun <reified T : MicrosmithExtension> with(value: T) = with(T::class, value)

    /** Return the set of extension keys currently attached to this model. */
    public fun keys(): Set<KClass<out MicrosmithExtension>> = extensions.keys

    public companion object {
        /** Create an empty model with no extensions. */
        public fun empty(): MicrosmithModel = MicrosmithModel(emptyMap())
    }
}
