package io.github.lmliam.microsmith.dsl.schemas.core

/**
 * Marker for all schema definitions.
 * Dialects implement this (protobuf, json, avro, etc.).
 *
 * @see SchemaType
 */
interface Schema {
    /**
     * The type of the schema, e.g. `protobuf`, `json`
     */
    val type: SchemaType

    /**
     * The name of the schema, e.g. `User`
     */
    val name: String
}

internal fun schemaKey(type: SchemaType, name: String): Pair<SchemaType, String> {
    require(name.isNotBlank()) { "Schema name cannot be blank." }
    return type to name
}

internal fun Schema.schemaKey(): Pair<SchemaType, String> = schemaKey(type, name)

internal fun schemaDisplayKey(type: SchemaType, name: String): String {
    schemaKey(type, name)
    return "${type.typeName}:$name"
}

internal fun Schema.schemaDisplayKey(): String = schemaDisplayKey(type, name)
