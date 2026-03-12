package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.core.SchemaType

enum class ProtobufSchemaType(override val typeName: String) : SchemaType {
    PROTOBUF("protobuf"),
}
