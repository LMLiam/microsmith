package me.liam.microsmith.dsl.schemas.core

internal class SchemasBuilder : SchemasScope {
    internal val schemas = mutableListOf<Schema>()

    fun register(schema: Schema) {
        val key = schema.name
        require(key.isNotBlank()) { "Schema name cannot be blank." }
        
        schemas += schema
    }

    fun build() = SchemasExtension(schemas.toList())
}