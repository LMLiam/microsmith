package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

data class Reference(val name: String, val type: Type? = null) : ValueType
