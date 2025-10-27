package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemaType

enum class ProtobufSchemaType(override val typeName: String) : SchemaType {
    PROTOBUF("protobuf")
}

data class ProtobufSchema(
    override val name: String,
    val message: Message? = null
) : Schema {
    override val type = ProtobufSchemaType.PROTOBUF
}