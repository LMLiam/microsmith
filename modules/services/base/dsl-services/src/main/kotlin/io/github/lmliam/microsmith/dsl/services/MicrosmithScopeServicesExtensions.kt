package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithScope
import io.github.lmliam.microsmith.dsl.put

/** Start a `services { ... }` block in the Microsmith DSL. */
fun MicrosmithScope.services(block: ServicesScope.() -> Unit) {
    val extension = ServicesBuilder().apply(block).toExtension()

    val builder =
        this as? MicrosmithBuilder ?: error("services { ... } can only be invoked within a MicrosmithBuilder scope.")

    builder.put(extension)
}
