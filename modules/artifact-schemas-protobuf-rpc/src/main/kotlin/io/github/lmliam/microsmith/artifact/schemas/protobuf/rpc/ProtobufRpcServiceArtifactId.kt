package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.core.ArtifactId

data class ProtobufRpcServiceArtifactId(
    val packageName: String?,
    val serviceName: String,
) : ArtifactId<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    val fullyQualifiedName: String = packageName?.let { "$it.$serviceName" } ?: serviceName
}
