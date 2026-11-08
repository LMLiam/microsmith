package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufValueType

internal object ProtobufValueTypeRenderer {
    @Suppress("CyclomaticComplexMethod")
    fun render(type: PrimitiveType): String =
        when (type) {
            PrimitiveType.INT32 -> "int32"
            PrimitiveType.INT64 -> "int64"
            PrimitiveType.UINT32 -> "uint32"
            PrimitiveType.UINT64 -> "uint64"
            PrimitiveType.SINT32 -> "sint32"
            PrimitiveType.SINT64 -> "sint64"
            PrimitiveType.FIXED32 -> "fixed32"
            PrimitiveType.FIXED64 -> "fixed64"
            PrimitiveType.SFIXED32 -> "sfixed32"
            PrimitiveType.SFIXED64 -> "sfixed64"
            PrimitiveType.STRING -> "string"
            PrimitiveType.BOOL -> "bool"
            PrimitiveType.FLOAT -> "float"
            PrimitiveType.DOUBLE -> "double"
            PrimitiveType.BYTES -> "bytes"
        }

    fun render(type: ResolvedProtobufValueType): String =
        when (type) {
            is ResolvedProtobufValueType.Primitive -> render(type.type)
            is ResolvedProtobufValueType.Reference -> type.reference.target.fullyQualifiedName
        }
}
