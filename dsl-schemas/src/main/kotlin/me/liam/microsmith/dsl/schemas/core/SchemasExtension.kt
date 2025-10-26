package me.liam.microsmith.dsl.schemas.core

import me.liam.microsmith.dsl.core.MicrosmithExtension

/**
 * Root extension that holds all declared schemas.
 */
data class SchemasExtension(val schemas: List<Schema>) : MicrosmithExtension {
    fun find(name: String) = schemas.find { it.name == name }
    fun require(name: String) = find(name) ?: error("Schema with name '$name' not found.")
}
