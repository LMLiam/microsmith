package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef

@MicrosmithDsl
interface RpcScope {
    fun request(target: String, block: RpcEndpointScope.() -> Unit = {})

    fun request(target: MessageRef, block: RpcEndpointScope.() -> Unit = {})

    fun response(target: String, block: RpcEndpointScope.() -> Unit = {})

    fun response(target: MessageRef, block: RpcEndpointScope.() -> Unit = {})

    fun stream(target: String): RpcEndpointMarker

    fun stream(target: MessageRef): RpcEndpointMarker

    infix fun String.to(other: String)

    infix fun String.to(other: MessageRef)

    infix fun String.to(other: RpcEndpointMarker)

    infix fun MessageRef.to(other: String)

    infix fun MessageRef.to(other: MessageRef)

    infix fun MessageRef.to(other: RpcEndpointMarker)

    infix fun RpcEndpointMarker.to(other: String)

    infix fun RpcEndpointMarker.to(other: MessageRef)

    infix fun RpcEndpointMarker.to(other: RpcEndpointMarker)
}
