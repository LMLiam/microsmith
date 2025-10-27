package me.liam.microsmith.dsl.schemas.protobuf

data class Enum(
    val name: String,
    val values: Set<EnumValue>
)