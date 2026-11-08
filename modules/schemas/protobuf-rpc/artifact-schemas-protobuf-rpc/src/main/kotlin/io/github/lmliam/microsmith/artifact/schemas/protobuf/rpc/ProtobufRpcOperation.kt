package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

data class ProtobufRpcOperation(val name: String, val request: ProtobufRpcEndpoint, val response: ProtobufRpcEndpoint)
