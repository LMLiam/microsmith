package me.liam.microsmith.dsl.schemas.core

import me.liam.microsmith.dsl.core.MicrosmithBuilder
import me.liam.microsmith.dsl.core.MicrosmithScope
import me.liam.microsmith.dsl.helpers.put

/**
 * Start a `schemas { ... }` block in the Microsmith DSL.
 */
fun MicrosmithScope.schemas(block: SchemasScope.() -> Unit) {
    val builder = SchemasBuilder().apply(block)
    val newExt = builder.toExtension()

    val microsmithBuilder =
        this as? MicrosmithBuilder
            ?: error("schemas { ... } can only be invoked within a MicrosmithBuilder scope.")
    val existing = microsmithBuilder.model.get<SchemasExtension>()

    if (existing != null) {
        microsmithBuilder.put(existing.merge(newExt))
    } else {
        microsmithBuilder.put(newExt)
    }
}
