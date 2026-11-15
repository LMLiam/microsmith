package io.github.lmliam.microsmith.dsl

/**
 * Marker for DSL extensions that can merge with another instance of the same type.
 *
 * This is used for additive DSL blocks where later declarations should extend earlier ones instead of silently
 * replacing them.
 */
public interface MergeableExtension<T : ModelExtension> : ModelExtension {
    public fun merge(other: T): T
}
