package me.liam.microsmith.dsl.schemas.protobuf.field

data class OneofField(
    override val name: String, override val index: Int, val fieldType: ValueType
) : Field