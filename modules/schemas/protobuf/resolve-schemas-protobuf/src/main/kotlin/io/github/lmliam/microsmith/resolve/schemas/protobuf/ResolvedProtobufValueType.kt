package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType

public sealed interface ResolvedProtobufValueType {
    public data class Primitive(public val type: PrimitiveType) : ResolvedProtobufValueType

    public data class Reference(public val reference: ResolvedProtobufReference) : ResolvedProtobufValueType
}
