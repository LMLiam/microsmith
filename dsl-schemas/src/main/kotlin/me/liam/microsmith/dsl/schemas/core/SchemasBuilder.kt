package me.liam.microsmith.dsl.schemas.core

import me.liam.microsmith.dsl.schemas.collision.CollisionPolicy
import me.liam.microsmith.dsl.schemas.collision.handleCollision

internal class SchemasBuilder(
    override var policy: CollisionPolicy = CollisionPolicy.ERROR
) : SchemasScope {
    internal val schemas = mutableListOf<Schema>()

    fun register(schema: Schema) {
        val key = schema.name
        require(key.isNotBlank()) { "Schema name cannot be blank." }

        if (!handleCollision(key)) return
        schemas += schema
    }

    fun build() = SchemasExtension(schemas.toList())
}