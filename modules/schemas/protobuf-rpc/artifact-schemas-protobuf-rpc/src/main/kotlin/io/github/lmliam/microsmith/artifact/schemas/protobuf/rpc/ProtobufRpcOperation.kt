package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

public data class ProtobufRpcOperation(
    public val name: String,
    public val request: ProtobufRpcEndpoint,
    public val response: ProtobufRpcEndpoint,
)
