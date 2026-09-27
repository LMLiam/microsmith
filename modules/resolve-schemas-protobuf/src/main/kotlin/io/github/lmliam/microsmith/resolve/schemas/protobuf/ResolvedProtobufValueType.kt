package io.github.lmliam.microsmith.resolve.schemas.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType

sealed interface ResolvedProtobufValueType {
    data class Primitive(val type: PrimitiveType) : ResolvedProtobufValueType
    data class Reference(val reference: ResolvedProtobufReference) : ResolvedProtobufValueType
}
