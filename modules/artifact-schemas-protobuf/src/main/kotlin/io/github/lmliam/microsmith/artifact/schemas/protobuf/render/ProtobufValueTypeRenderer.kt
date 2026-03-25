package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.invariantViolation
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ValueType

internal object ProtobufValueTypeRenderer {
    private val primitiveKeywords =
        mapOf(
            PrimitiveType.INT32 to "int32",
            PrimitiveType.INT64 to "int64",
            PrimitiveType.UINT32 to "uint32",
            PrimitiveType.UINT64 to "uint64",
            PrimitiveType.SINT32 to "sint32",
            PrimitiveType.SINT64 to "sint64",
            PrimitiveType.FIXED32 to "fixed32",
            PrimitiveType.FIXED64 to "fixed64",
            PrimitiveType.SFIXED32 to "sfixed32",
            PrimitiveType.SFIXED64 to "sfixed64",
            PrimitiveType.STRING to "string",
            PrimitiveType.BOOL to "bool",
            PrimitiveType.FLOAT to "float",
            PrimitiveType.DOUBLE to "double",
            PrimitiveType.BYTES to "bytes",
        )

    fun render(type: PrimitiveType): String =
        primitiveKeywords[type] ?: invariantViolation("Unsupported protobuf primitive type: $type")

    fun render(type: ValueType): String = when (type) {
        is PrimitiveType -> render(type)
        is Reference -> type.name
    }

    fun render(type: MapType): String = "map<${render(type.key)}, ${render(type.value)}>"
}
