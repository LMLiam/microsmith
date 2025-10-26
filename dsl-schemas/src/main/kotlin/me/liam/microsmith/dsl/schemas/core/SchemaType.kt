package me.liam.microsmith.dsl.schemas.core

/**
 * The type of schema.
 *
 * This sealed interface acts as a marker for dialect identifiers
 * (e.g. `protobuf`, `json`, `avro`). By requiring implementors to
 * provide a `typeName`, developers are pushed into using a strongly
 * typed, canonical representation of schema types rather than
 * arbitrary strings.
 *
 * Typical usage is to define an `enum class` that implements [SchemaType],
 * ensuring that only a fixed set of dialects can be referenced in your DSL.
 * Each dialect then associates its schemas with the appropriate enum constant.
 *
 * ### Example
 * ```kotlin
 * enum class SchemaTypes(override val typeName: String) : SchemaType {
 *     PROTOBUF("protobuf")
 * }
 *
 * data class ProtobufSchema(override val name: String) : Schema {
 *     override val type = SchemaTypes.PROTOBUF
 * }
 * ```
 * In this example, the `ProtobufSchema` is tied to the `PROTOBUF`
 * schema type, guaranteeing that all protobuf schemas are consistently
 * identified by the `"protobuf"` type name.
 *
 * @see Schema
 */
sealed interface SchemaType {
    val typeName: String
}