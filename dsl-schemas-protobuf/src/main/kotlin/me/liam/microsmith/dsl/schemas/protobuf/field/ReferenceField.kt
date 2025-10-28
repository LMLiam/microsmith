package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.types.Type

data class Reference(
    val name: String
) : ValueType {
    lateinit var type: Type
}

data class ReferenceField(
    override val name: String,
    override val index: Int,
    val reference: Reference
) : Field
