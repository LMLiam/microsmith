package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class Message(
    override val name: String,
    val fields: List<Field> = emptyList(),
    val oneofs: List<Oneof> = emptyList(),
    val reserved: List<Reserved> = emptyList()
) : Type
