package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface RpcEndpointScope {
    fun stream()
}
