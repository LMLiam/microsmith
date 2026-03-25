package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

data class ResolvedProtobufRpcEndpoint(
    val qualifiedTypeName: String,
    val streaming: Boolean,
)
