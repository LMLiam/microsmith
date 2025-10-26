package me.liam.microsmith.dsl.schemas.helpers

import me.liam.microsmith.dsl.schemas.collision.CollisionPolicy
import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemasBuilder
import me.liam.microsmith.dsl.schemas.core.SchemasScope

fun SchemasScope.group(prefix: String, block: SchemasScope.() -> Unit) {
    val builder = this as SchemasBuilder
    block(object : SchemasScope {
        override val policy: CollisionPolicy
            get() = builder.policy

        fun register(schema: Schema) {
            builder.register(object : Schema {
                override val name = "$prefix.${schema.name}"
            })
        }
    })
}