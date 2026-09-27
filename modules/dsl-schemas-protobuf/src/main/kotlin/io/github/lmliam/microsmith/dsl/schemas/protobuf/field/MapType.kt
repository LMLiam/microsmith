package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

data class MapType(val key: MapKeyType, val value: ValueType) : FieldType
