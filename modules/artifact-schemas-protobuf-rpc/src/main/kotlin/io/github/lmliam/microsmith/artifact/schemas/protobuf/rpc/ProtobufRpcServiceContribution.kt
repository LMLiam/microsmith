package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.core.ArtifactContribution

data class ProtobufRpcServiceContribution(
    override val artifactId: ProtobufRpcServiceArtifactId,
    val imports: List<String> = emptyList(),
    val operations: List<ProtobufRpcOperation>,
) : ArtifactContribution<ProtobufRpcServiceArtifact> {
    init {
        require(operations.isNotEmpty()) {
            "Protobuf RPC service contributions must declare at least one RPC operation."
        }
    }
}
