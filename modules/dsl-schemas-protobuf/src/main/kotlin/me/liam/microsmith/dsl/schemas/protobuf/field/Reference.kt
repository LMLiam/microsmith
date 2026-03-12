package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.types.Type

data class Reference(val name: String, val type: Type? = null) : ValueType
