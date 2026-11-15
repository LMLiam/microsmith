package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

public data class ResolvedProtobufRpc(
    public val name: ProtoIdentifier,
    public val request: ResolvedProtobufRpcEndpoint,
    public val response: ResolvedProtobufRpcEndpoint,
)
