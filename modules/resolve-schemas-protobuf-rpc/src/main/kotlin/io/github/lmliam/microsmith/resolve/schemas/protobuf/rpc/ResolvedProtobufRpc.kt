package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

data class ResolvedProtobufRpc(
    val name: String,
    val request: ResolvedProtobufRpcEndpoint,
    val response: ResolvedProtobufRpcEndpoint,
)
