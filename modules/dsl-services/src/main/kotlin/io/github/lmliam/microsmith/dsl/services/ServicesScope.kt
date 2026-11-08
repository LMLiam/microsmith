package io.github.lmliam.microsmith.dsl.services

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

/**
 * Marker interface for the `services { ... }` DSL block.
 *
 * Feature modules extend this scope with shared service configuration entrypoints, while the core module contributes
 * named service declarations.
 */
@MicrosmithDsl
interface ServicesScope {
    operator fun String.invoke(block: ServiceScope.() -> Unit = {})
}
