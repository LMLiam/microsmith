package io.github.lmliam.microsmith.dsl.core

/**
 * Marker for DSL extensions that can merge with another instance of the same type.
 *
 * This is used for additive DSL blocks where later declarations should extend
 * earlier ones instead of silently replacing them.
 */
interface MergeableExtension<T : ModelExtension> : ModelExtension {
    fun merge(other: T): T
}
