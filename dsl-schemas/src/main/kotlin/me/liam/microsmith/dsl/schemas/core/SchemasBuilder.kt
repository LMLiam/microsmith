package me.liam.microsmith.dsl.schemas.core

/**
 * Internal builder used within the `schemas { ... }` DSL block.
 *
 * This class is responsible for collecting all [Schema] instances
 * declared by dialect-specific DSL functions (e.g. `protobuf { ... }`,
 * `json { ... }`). It provides a simple [register] method to add new
 * schemas and performs basic validation (e.g. schema names must not
 * be blank).
 *
 * The builder itself is mutable, but the result of [toExtension] is an
 * immutable [SchemasExtension] that is attached to the overall
 * [me.liam.microsmith.dsl.core.MicrosmithModel] via the
 * [me.liam.microsmith.dsl.core.MicrosmithBuilder].
 *
 * ### Lifecyce
 * - A new [SchemasBuilder] is created when entering a `schemas { }` block.
 * - Dialect DSL functions downcast the [SchemasScope] to [SchemasBuilder]
 *   and call [register] to contribute their definitions.
 * - Once the block completes, [toExtension] is invoked to produce a
 *   [SchemasExtension] snapshot containing all registered schemas.
 *
 * ### Example
 * ```kotlin
 * fun SchemasScope.protobuf(name: String, block: ProtobufSchemaBuilder.() -> Unit) {
 *   val builder = this as SchemasBuilder
 *   builder.register(ProtobufSchema(name, block))
 * ```
 *
 * End-users never interact with [SchemasBuilder] directly; it is purely
 * an internal mechanism to accumulate schemas during DSL evaluation.
 */
class SchemasBuilder : SchemasScope {
    internal val schemas = mutableSetOf<Schema>()

    /**
     * Register a new [Schema] with this builder.
     *
     * @throws IllegalArgumentException if the schema name is blank.
     */
    fun register(schema: Schema) {
        val key = schema.name
        require(key.isNotBlank()) { "Schema name cannot be blank." }

        schemas += schema
    }

    /**
     * Finalise the builder and produce an immutable [SchemasExtension]
     * containing all registered schemas.
     */
    fun toExtension() = SchemasExtension(schemas.toSet())
}