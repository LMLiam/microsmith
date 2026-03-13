package io.github.lmliam.microsmith.dsl.schemas.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension

/**
 * Root extension that holds all declared schemas.
 */
data class SchemasExtension(val schemas: Set<Schema>) : MicrosmithExtension {
    init {
        val duplicateKeys =
            schemas
                .groupBy(SchemaKey::of)
                .filterValues { it.size > 1 }
                .keys
                .map(SchemaKey::toString)
                .sorted()

        require(duplicateKeys.isEmpty()) {
            "SchemasExtension contains duplicate schema keys: ${duplicateKeys.joinToString(", ")}"
        }
    }

    // Precompute an index for efficient lookups
    private val index = schemas.associateBy(SchemaKey::of)

    /**
     * Find a schema by [type] and [name].
     *
     * @return the matching [Schema], or `null` if not found.
     */
    fun find(type: SchemaType, name: String) = index[SchemaKey(type, name)]

    /**
     * Require a schema by [type] and [name].
     *
     * @throws IllegalStateException if no schema with the given
     * type and name exists.
     */
    fun require(type: SchemaType, name: String) =
        find(type, name) ?: error("Schema not found: ${SchemaKey(type, name)}")

    /**
     * Convenience: return all schemas of a given [type].
     */
    fun allOf(type: SchemaType) = schemas.filter { it.type == type }.toSet()

    fun merge(other: SchemasExtension): SchemasExtension {
        val existingKeys = schemas.mapTo(mutableSetOf(), SchemaKey::of)
        val collisions =
            other.schemas
                .map(SchemaKey::of)
                .filter { it in existingKeys }
                .map(SchemaKey::toString)
                .distinct()
                .sorted()

        require(collisions.isEmpty()) {
            "Duplicate schema keys while merging SchemasExtension: ${collisions.joinToString(", ")}"
        }

        return copy(schemas = schemas + other.schemas)
    }
}
