package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

data class ResolvedProtobufOneof(val name: ProtoIdentifier, val fields: List<Field>) {
    data class Field(val name: ProtoIdentifier, val number: FieldNumber, val type: ResolvedProtobufValueType)
}
