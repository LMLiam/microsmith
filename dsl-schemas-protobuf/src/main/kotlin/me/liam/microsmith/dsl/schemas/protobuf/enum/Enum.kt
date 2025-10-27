package me.liam.microsmith.dsl.schemas.protobuf.enum

import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class Enum(
    val name: String,
    val values: List<EnumValue>,
    val reserved: List<Reserved> = emptyList()
) {
    companion object {
        internal const val UNSPECIFIED = "UNSPECIFIED"
    }
}