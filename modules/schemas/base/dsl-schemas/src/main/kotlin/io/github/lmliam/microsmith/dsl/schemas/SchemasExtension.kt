package io.github.lmliam.microsmith.dsl.schemas

import io.github.lmliam.microsmith.dsl.MergeableExtension
import io.github.lmliam.microsmith.dsl.MicrosmithExtension

/** Root extension that holds all declared schemas. */
public data class SchemasExtension(public val schemas: Set<Schema>) :
    MicrosmithExtension, MergeableExtension<SchemasExtension> {
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
    public fun find(type: SchemaType, name: String): Schema? = index[schemaKey(type, name)]

    /**
     * Require a schema by [type] and [name].
     *
     * @throws IllegalStateException if no schema with the given type and name exists.
     */
    public fun require(type: SchemaType, name: String): Schema =
        find(type, name) ?: error("Schema not found: ${schemaDisplayKey(type, name)}")

    /** Convenience: return all schemas of a given [type]. */
    public fun allOf(type: SchemaType): Set<Schema> = schemas.filter { it.type == type }.toSet()

    override fun merge(other: SchemasExtension): SchemasExtension {
        val existingKeys = schemas.mapTo(mutableSetOf(), Schema::schemaKey)
        val collisions =
            other.schemas
                .asSequence()
                .map(Schema::schemaKey)
                .filter { it in existingKeys }
                .map { (type, name) -> schemaDisplayKey(type, name) }
                .distinct()
                .sorted()
                .toList()

        require(collisions.isEmpty()) {
            "Duplicate schema keys while merging SchemasExtension: ${collisions.joinToString(", ")}"
        }

        return copy(schemas = schemas + other.schemas)
    }
}
