package me.liam.microsmith.dsl.schemas.protobuf.oneof

import me.liam.microsmith.dsl.schemas.protobuf.field.Field

data class Oneof(
    val name: String,
    val fields: List<Field>
)
