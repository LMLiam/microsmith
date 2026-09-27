package io.github.lmliam.microsmith.dsl.schemas
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithScope
import io.github.lmliam.microsmith.dsl.put

/**
 * Start a `schemas { ... }` block in the Microsmith DSL.
 */
fun MicrosmithScope.schemas(block: SchemasScope.() -> Unit) {
    val extension = SchemasBuilder()
        .apply(block)
        .toExtension()

    val builder = this as? MicrosmithBuilder
        ?: error("schemas { ... } can only be invoked within a MicrosmithBuilder scope")

    builder.put(extension)
}
