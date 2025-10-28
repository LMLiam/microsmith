package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.types.Type

sealed class Reference(
    open val type: Type
) : ValueType {
    val name get() = type.name
}

data class ReferenceField(
    override val name: String,
    override val index: Int,
    val reference: Reference
) : Field
