package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.Schema
import io.github.lmliam.microsmith.dsl.schemas.SchemaType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

data class ProtobufSchema(override val name: String, val schema: Type) : Schema {
    override val type: SchemaType
        get() = ProtobufSchemaType.PROTOBUF
}
