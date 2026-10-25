package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

data class Service(override val name: String, val rpcs: List<Rpc>) : Type
