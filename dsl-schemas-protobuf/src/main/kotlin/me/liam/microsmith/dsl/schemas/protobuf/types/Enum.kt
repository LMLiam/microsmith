package me.liam.microsmith.dsl.schemas.protobuf.types

import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class Enum(
    override val name: String,
    val values: List<EnumValue>,
    override val reserved: List<Reserved> = emptyList(),
) : Type, ReservedDeclarationOwner {
    companion object {
        internal const val UNSPECIFIED = "UNSPECIFIED"
    }
}
