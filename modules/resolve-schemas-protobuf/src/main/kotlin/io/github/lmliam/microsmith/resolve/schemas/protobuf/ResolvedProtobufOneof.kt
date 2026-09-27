package io.github.lmliam.microsmith.resolve.schemas.protobuf

data class ResolvedProtobufOneof(val name: String, val fields: List<Field>) {
    data class Field(val name: String, val number: Int, val type: ResolvedProtobufValueType)
}
