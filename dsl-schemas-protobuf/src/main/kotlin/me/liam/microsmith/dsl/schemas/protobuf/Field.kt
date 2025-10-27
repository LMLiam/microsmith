package me.liam.microsmith.dsl.schemas.protobuf

data class Field(
    val name: String,
    val type: FieldType,
    val index: Int,
    val optional: Boolean = false
)
