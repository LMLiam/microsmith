package me.liam.microsmith.dsl.schemas.core

import me.liam.microsmith.dsl.core.MicrosmithBuilder
import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.core.MicrosmithScope
import me.liam.microsmith.dsl.helpers.put

/**
 * Marker interface for the `schemas { ... }` DSL block.
 *
 * This scope is intentionally empty: it does not define any functions
 * itself. Instead, individual dialect modules (e.g. `dsl-schemas-protobuf`)
 * contribute extension functions on [SchemasScope]
 * such as `protobuf("User") { ... }`.
 *
 * End-users never implement this interface directly; they only encounter
 * it when writing DSL blocks.
 */
@MicrosmithDsl
interface SchemasScope

/**
 * Start a `schemas { ... }` block in your DSL.
 *
 * Inside this block you can declare schemas using dialect‑specific
 * functions such as `protobuf("User") { ... }` or `json("Order") { ... }`.
 *
 * **Note:** see dialect specific documentation for more information.
 *
 * Example:
 * ```
 * microsmith {
 *     schemas {
 *         protobuf("User") {
 *             // define protobuf schema here
 *         }
 *         json("Order") {
 *             // define JSON schema here
 *         }
 *     }
 * }
 * ```
 *
 * The schemas you declare here are collected and made available
 * to the rest of your Microsmith model.
 */
fun MicrosmithScope.schemas(block: SchemasScope.() -> Unit) {
    val builder = SchemasBuilder().apply(block)
    val newExt = builder.build()

    val msBuilder = this as MicrosmithBuilder
    val existing = msBuilder.model.get<SchemasExtension>()

    if (existing != null) {
        msBuilder.put(existing.merge(newExt))
    } else {
        msBuilder.put(newExt)
    }
}