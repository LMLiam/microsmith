package me.liam.microsmith.dsl.schemas.protobuf.message

import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.field.Field

data class Message(
    val name: String,
    val fields: Set<Field> = emptySet(),
    val oneofs: Set<Oneof> = emptySet()
)
