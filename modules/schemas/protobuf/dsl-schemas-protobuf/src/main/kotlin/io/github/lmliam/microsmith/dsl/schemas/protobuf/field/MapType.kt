package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public data class MapType(val key: MapKeyType, val value: ValueType) : FieldType
