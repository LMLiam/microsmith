package me.liam.microsmith.dsl.schemas.protobuf

data class Message(
    val name: String,
    val fields: Set<Field> = emptySet(),
    val oneofs: Set<Oneof> = emptySet()
)
