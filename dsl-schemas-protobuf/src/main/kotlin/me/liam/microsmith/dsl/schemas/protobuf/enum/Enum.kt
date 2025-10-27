package me.liam.microsmith.dsl.schemas.protobuf.enum

data class Enum(
    val name: String,
    val values: List<EnumValue>
)