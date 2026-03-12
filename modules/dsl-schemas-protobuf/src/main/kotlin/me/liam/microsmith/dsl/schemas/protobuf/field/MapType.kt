package me.liam.microsmith.dsl.schemas.protobuf.field

data class MapType(val key: MapKeyType, val value: ValueType) : FieldType
