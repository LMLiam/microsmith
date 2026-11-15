package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef

@MicrosmithDsl
public interface RpcScope {
    public fun request(target: String, block: RpcEndpointScope.() -> Unit = {})

    public fun request(target: MessageRef, block: RpcEndpointScope.() -> Unit = {})

    public fun response(target: String, block: RpcEndpointScope.() -> Unit = {})

    public fun response(target: MessageRef, block: RpcEndpointScope.() -> Unit = {})

    public fun stream(target: String): RpcEndpointMarker

    public fun stream(target: MessageRef): RpcEndpointMarker

    public infix fun String.to(other: String)

    public infix fun String.to(other: MessageRef)

    public infix fun String.to(other: RpcEndpointMarker)

    public infix fun MessageRef.to(other: String)

    public infix fun MessageRef.to(other: MessageRef)

    public infix fun MessageRef.to(other: RpcEndpointMarker)

    public infix fun RpcEndpointMarker.to(other: String)

    public infix fun RpcEndpointMarker.to(other: MessageRef)

    public infix fun RpcEndpointMarker.to(other: RpcEndpointMarker)
}
