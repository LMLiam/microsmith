package io.github.lmliam.microsmith.dsl

/**
 * DSL entrypoint for end-users.
 *
 * Returns the immutable [MicrosmithModel] built from the DSL.
 */
public fun microsmith(block: MicrosmithScope.() -> Unit): MicrosmithModel = MicrosmithBuilder().apply(block).model
