package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.SchemaType

internal enum class ProtobufSchemaType(override val typeName: String) : SchemaType {
    PROTOBUF("protobuf"),
}
