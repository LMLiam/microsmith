package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public data class OneofField(override val name: String, override val index: Int, val fieldType: ValueType) : Field
