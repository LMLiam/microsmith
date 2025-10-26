package me.liam.microsmith.dsl.core

/**
 * DSL marker annotation to prevent accidental scope leakage between nested DSL blocks.
 *
 * Apply this to all DSL scope interfaces (e.g. [MicrosmithScope], service scopes, etc.).
 */
@DslMarker
annotation class MicrosmithDsl