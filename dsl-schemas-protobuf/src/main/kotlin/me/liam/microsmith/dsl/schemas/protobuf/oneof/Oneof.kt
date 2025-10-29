package me.liam.microsmith.dsl.schemas.protobuf.oneof

import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField

data class Oneof(
    val name: String, val fields: List<OneofField>
)
