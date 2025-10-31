package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemaType
import me.liam.microsmith.dsl.schemas.protobuf.types.Type

enum class ProtobufSchemaType(
    override val typeName: String
) : SchemaType {
    PROTOBUF("protobuf")
}

data class ProtobufSchema(
    override val name: String,
    val schema: Type
) : Schema {
    override val type: SchemaType get() = ProtobufSchemaType.PROTOBUF
}