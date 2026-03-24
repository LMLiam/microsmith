package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.core.MicrosmithScope
import io.github.lmliam.microsmith.dsl.helpers.put

/**
 * Start a `services { ... }` block in the Microsmith DSL.
 */
fun MicrosmithScope.services(block: ServicesScope.() -> Unit) {
    val builder = ServicesBuilder().apply(block)
    val newExt = builder.toExtension()

    val microsmithBuilder =
        this as? MicrosmithBuilder
            ?: error("services { ... } can only be invoked within a MicrosmithBuilder scope.")
    val existing = microsmithBuilder.model.get<ServicesExtension>()

    if (existing != null) {
        microsmithBuilder.put(existing.merge(newExt))
    } else {
        microsmithBuilder.put(newExt)
    }
}
