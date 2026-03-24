package io.github.lmliam.microsmith.dsl.helpers

import io.github.lmliam.microsmith.dsl.core.MergeableExtension
import io.github.lmliam.microsmith.dsl.core.ModelExtension

/**
 * Merge [incoming] into [existing] when the extension type opts into mergeable
 * semantics, otherwise prefer the incoming value.
 */
@Suppress("UNCHECKED_CAST")
fun <T : ModelExtension> mergeModelExtension(existing: T?, incoming: T): T = when {
    existing == null -> incoming
    existing::class == incoming::class && existing is MergeableExtension<*> ->
        (existing as MergeableExtension<T>).merge(incoming)
    else -> incoming
}
