package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

data class ProtobufRpcOperation(
    val name: String,
    val requestTypeName: String,
    val requestStreaming: Boolean,
    val responseTypeName: String,
    val responseStreaming: Boolean,
)
