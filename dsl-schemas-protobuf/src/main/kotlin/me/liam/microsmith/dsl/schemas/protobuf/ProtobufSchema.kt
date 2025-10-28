package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.core.Schema
import me.liam.microsmith.dsl.schemas.core.SchemaType

enum class ProtobufSchemaType(override val typeName: String) : SchemaType {
    PROTOBUF("protobuf")
}

sealed interface ProtobufSchema : Schema {
    override val type: SchemaType get() = ProtobufSchemaType.PROTOBUF
}

data class ProtobufMessageSchema(
    override val name: String,
    val message: Message
) : ProtobufSchema

data class ProtobufEnumSchema(
    override val name: String,
    val enum: Enum
) : ProtobufSchema