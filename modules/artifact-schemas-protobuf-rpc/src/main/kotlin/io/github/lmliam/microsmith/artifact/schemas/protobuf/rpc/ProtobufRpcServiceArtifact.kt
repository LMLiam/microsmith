package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.core.ProtobufRpcArtifact

data class ProtobufRpcServiceArtifact(
    override val id: ProtobufRpcServiceArtifactId,
    val imports: List<String>,
    val operations: List<ProtobufRpcOperation>,
) : ProtobufRpcArtifact
