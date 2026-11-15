package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcArtifact

public data class ProtobufRpcServiceArtifact(
    public override val id: ProtobufRpcServiceArtifactId,
    public val imports: List<String>,
    public val operations: List<ProtobufRpcOperation>,
) : ProtobufRpcArtifact
