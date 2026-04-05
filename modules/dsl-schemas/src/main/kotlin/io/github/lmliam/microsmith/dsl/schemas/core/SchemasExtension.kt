package io.github.lmliam.microsmith.dsl.schemas.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension

/**
 * Root extension that holds all declared schemas.
 */
data class SchemasExtension(val schemas: Set<Schema>) : MicrosmithExtension {
    init {
        val duplicateKeys =
            schemas
                .groupBy(Schema::schemaKey)
                .filterValues { it.size > 1 }
                .keys
                .map { (type, name) -> schemaDisplayKey(type, name) }
                .sorted()

        require(duplicateKeys.isEmpty()) {
            "SchemasExtension contains duplicate schema keys: ${duplicateKeys.joinToString(", ")}"
        }
    }

    // Precompute an index for efficient lookups
    private val index = schemas.associateBy(Schema::schemaKey)

    /**
     * Find a schema by [type] and [name].
     *
     * @return the matching [Schema], or `null` if not found.
     */
    fun find(type: SchemaType, name: String) = index[schemaKey(type, name)]

    /**
     * Require a schema by [type] and [name].
     *
     * @throws IllegalStateException if no schema with the given
     * type and name exists.
     */
    fun require(type: SchemaType, name: String) =
        find(type, name) ?: error("Schema not found: ${schemaDisplayKey(type, name)}")

    /**
     * Convenience: return all schemas of a given [type].
     */
    fun allOf(type: SchemaType) = schemas.filter { it.type == type }.toSet()

    fun merge(other: SchemasExtension): SchemasExtension {
        val existingKeys = schemas.mapTo(mutableSetOf(), Schema::schemaKey)
        val collisions =
            other.schemas
                .map(Schema::schemaKey)
                .filter { it in existingKeys }
                .map { (type, name) -> schemaDisplayKey(type, name) }
                .distinct()
                .sorted()

        require(collisions.isEmpty()) {
            "Duplicate schema keys while merging SchemasExtension: ${collisions.joinToString(", ")}"
        }

        return copy(schemas = schemas + other.schemas)
    }
}
