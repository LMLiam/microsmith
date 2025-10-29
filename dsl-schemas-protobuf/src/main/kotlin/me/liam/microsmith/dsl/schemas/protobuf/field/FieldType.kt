package me.liam.microsmith.dsl.schemas.protobuf.field

sealed interface FieldType

sealed interface ValueType : FieldType

sealed interface PrimitiveType : ValueType {
    object INT32 : MapKeyType
    object INT64 : MapKeyType
    object UINT32 : MapKeyType
    object UINT64 : MapKeyType
    object SINT32 : MapKeyType
    object SINT64 : MapKeyType
    object FIXED32 : MapKeyType
    object FIXED64 : MapKeyType
    object SFIXED32 : MapKeyType
    object SFIXED64 : MapKeyType
    object STRING : MapKeyType
    object BOOL : MapKeyType
    object FLOAT : PrimitiveType
    object DOUBLE : PrimitiveType
    object BYTES : PrimitiveType
}

sealed interface MapKeyType : PrimitiveType

data class MapType(
    val key: MapKeyType, val value: ValueType
) : FieldType