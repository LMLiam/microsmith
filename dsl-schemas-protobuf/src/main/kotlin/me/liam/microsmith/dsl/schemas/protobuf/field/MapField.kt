package me.liam.microsmith.dsl.schemas.protobuf.field

data class MapField(
    override val name: String, override val index: Int, val type: MapType
) : Field