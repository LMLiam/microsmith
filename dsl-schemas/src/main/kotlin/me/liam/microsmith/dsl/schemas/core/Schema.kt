package me.liam.microsmith.dsl.schemas.core

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