package me.liam.microsmith.dsl.schemas.protobuf

data class Oneof(
    val name: String,
    val fields: Set<Field>
)
