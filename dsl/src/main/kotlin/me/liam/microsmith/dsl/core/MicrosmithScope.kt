package me.liam.microsmith.dsl.core

/**
 * Root DSL scope visible to end-users.
 *
 * Plugin authors extend this scope with their own DSL entrypoints
 * (e.g. `fun MicrosmithScope.services { ... }`).
 */
@MicrosmithDsl
interface MicrosmithScope

/**
 * DSL entrypoint for end-users.
 *
 * Example in a .kts file:
 * ```
 * microsmith {
 *   services { ... } // hypothetical DSL for services
 * }
 * ```
 *
 * Returns the immutable [MicrosmithModel] built from the DSL.
 */
fun microsmith(block: MicrosmithScope.() -> Unit) = MicrosmithBuilder().apply(block).model