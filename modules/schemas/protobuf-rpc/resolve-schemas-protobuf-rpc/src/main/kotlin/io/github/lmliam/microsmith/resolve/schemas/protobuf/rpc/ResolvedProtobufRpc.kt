package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

data class ResolvedProtobufRpc(
    val name: ProtoIdentifier,
    val request: ResolvedProtobufRpcEndpoint,
    val response: ResolvedProtobufRpcEndpoint,
)
