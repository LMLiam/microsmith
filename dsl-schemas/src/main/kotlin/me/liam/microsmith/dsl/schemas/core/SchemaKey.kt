package me.liam.microsmith.dsl.schemas.core

internal data class SchemaKey(val type: SchemaType, val name: String) {
    init {
        require(name.isNotBlank()) { "Schema name cannot be blank." }
    }

    override fun toString() = "${type.typeName}:$name"

    companion object {
        fun of(schema: Schema) = SchemaKey(schema.type, schema.name)
    }
}
