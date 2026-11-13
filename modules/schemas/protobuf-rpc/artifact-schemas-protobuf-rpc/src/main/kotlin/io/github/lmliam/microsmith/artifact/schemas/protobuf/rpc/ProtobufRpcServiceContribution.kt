package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.ArtifactContribution

data class ProtobufRpcServiceContribution(
    override val artifactId: ProtobufRpcServiceArtifactId,
    val imports: List<String> = emptyList(),
    val operations: List<ProtobufRpcOperation>,
) : ArtifactContribution<ProtobufRpcServiceArtifact>
