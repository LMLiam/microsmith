package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface RpcScope {
    fun request(target: String, block: RpcEndpointScope.() -> Unit = {})

    fun response(target: String, block: RpcEndpointScope.() -> Unit = {})

    fun stream(target: String): RpcEndpointMarker
}
