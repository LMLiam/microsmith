package me.liam.microsmith.dsl.schemas.collision

import me.liam.microsmith.dsl.schemas.core.SchemasBuilder

/**
 * Handles collisions for a given schema name during schema registration.
 * @return true if the schema should be added to the list of schemas.
 */
internal fun SchemasBuilder.handleCollision(key: String): Boolean {
    when (policy) {
        CollisionPolicy.ERROR -> {
            if (schemas.any { it.name == key }) {
                error("Schema with name '$key' is already registered.")
            }
        }

        CollisionPolicy.KEEP_FIRST -> {
            if (schemas.any { it.name == key }) {
                return false
            }
        }

        CollisionPolicy.REPLACE -> {
            schemas.removeIf { it.name == key }
            return true
        }
    }
    return true
}