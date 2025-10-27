package me.liam.microsmith.dsl.schemas.protobuf

sealed interface FieldType

sealed interface MapValueType : FieldType

sealed interface PrimitiveFieldType : MapValueType {
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
    object FLOAT : PrimitiveFieldType
    object DOUBLE : PrimitiveFieldType
    object BYTES : PrimitiveFieldType
}

sealed interface MapKeyType : PrimitiveFieldType

data class MapFieldType(
    val key: MapKeyType,
    val value: MapValueType
) : FieldType