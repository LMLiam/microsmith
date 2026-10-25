package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

data class Rpc(val name: String, val request: RpcEndpoint, val response: RpcEndpoint)
