package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public data class MapField(override val name: String, override val index: Int, val type: MapType) : MessageField
