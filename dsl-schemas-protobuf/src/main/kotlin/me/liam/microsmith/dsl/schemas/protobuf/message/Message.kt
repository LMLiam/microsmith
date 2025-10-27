package me.liam.microsmith.dsl.schemas.protobuf.message

import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class Message(
    val name: String,
    val fields: Set<Field> = emptySet(),
    val oneofs: Set<Oneof> = emptySet(),
    val reserved: Set<Reserved> = emptySet()
)
