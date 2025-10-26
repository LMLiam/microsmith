package me.liam.microsmith.dsl.schemas.core

import me.liam.microsmith.dsl.core.MicrosmithBuilder
import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.core.MicrosmithScope
import me.liam.microsmith.dsl.helpers.put
import me.liam.microsmith.dsl.schemas.collision.CollisionPolicy

/**
 * User-facing DSL scope for schemas { }.
 * Intentionally empty - dialects add extension functions here.
 */
@MicrosmithDsl
interface SchemasScope {
    val policy: CollisionPolicy
}

/**
 * DSL builder for [SchemasScope].
 */
fun MicrosmithScope.schemas(block: SchemasScope.() -> Unit) {
    val builder = SchemasBuilder().apply(block)
    (this as MicrosmithBuilder).put(builder.build())
}