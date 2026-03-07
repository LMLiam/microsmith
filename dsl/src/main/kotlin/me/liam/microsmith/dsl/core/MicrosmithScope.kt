package me.liam.microsmith.dsl.core

/**
 * Root DSL scope visible to end-users.
 *
 * Plugin authors extend this scope with their own DSL entrypoints
 * (for example, `fun MicrosmithScope.schemas { ... }`).
 */
@MicrosmithDsl
interface MicrosmithScope
