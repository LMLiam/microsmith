package me.liam.microsmith.dsl.core

/**
 * DSL entrypoint for end-users.
 *
 * Returns the immutable [MicrosmithModel] built from the DSL.
 */
fun microsmith(block: MicrosmithScope.() -> Unit) = MicrosmithBuilder().apply(block).model
