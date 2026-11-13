package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId

data class ProtobufRpcServiceArtifactId(val packageName: String?, val serviceName: String) :
    ArtifactId<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    val protoFile = ProtoFileArtifactId(packageName, serviceName)
}
