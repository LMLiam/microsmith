package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpc
import tech.mappie.api.ObjectMappie

internal object ProtobufRpcOperationMapper : ObjectMappie<ResolvedProtobufRpc, ProtobufRpcOperation>() {
    override fun map(from: ResolvedProtobufRpc): ProtobufRpcOperation = mapping {
        to::name fromProperty from::name transform { it.value }
        to::request fromProperty from::request via ProtobufRpcEndpointMapper
        to::response fromProperty from::response via ProtobufRpcEndpointMapper
    }
}
