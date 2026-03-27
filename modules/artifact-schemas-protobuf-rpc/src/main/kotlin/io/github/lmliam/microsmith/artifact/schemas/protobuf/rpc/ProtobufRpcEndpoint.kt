package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

data class ProtobufRpcEndpoint(
    val typeName: String,
    val streaming: Boolean,
)
