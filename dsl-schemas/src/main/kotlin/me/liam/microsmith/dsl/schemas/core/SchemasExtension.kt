package me.liam.microsmith.dsl.schemas.core

import me.liam.microsmith.dsl.core.MicrosmithExtension

/**
 * Root extension that holds all declared schemas.
 */
data class SchemasExtension(val schemas: Set<Schema>) : MicrosmithExtension {
    // Precompute an index for efficient lookups
    private val index =
        schemas.associateBy { it.type to it.name }

    /**
     * Find a schema by [type] and [name].
     *
     * @return the matching [Schema], or `null` if not found.
     */
    fun find(type: SchemaType, name: String) = index[type to name]

    /**
     * Require a schema by [type] and [name].
     *
     * @throws IllegalStateException if no schema with the given
     * type and name exists.
     */
    fun require(type: SchemaType, name: String) = find(type, name) ?: error("Schema not found: $type:$name")

    /**
     * Convenience: return all schemas of a given [type].
     */
    fun allOf(type: SchemaType) = schemas.filter { it.type == type }.toSet()

    fun merge(other: SchemasExtension): SchemasExtension {
        val merged = (schemas + other.schemas)
            .associateBy { it.type to it.name }
            .values.toSet()
        return copy(schemas = merged)
    }
}
