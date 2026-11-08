package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

data class ResolvedProtobufOneof(val name: ProtoIdentifier, val fields: List<Field>) {
    data class Field(val name: ProtoIdentifier, val number: Int, val type: ResolvedProtobufValueType)
}
