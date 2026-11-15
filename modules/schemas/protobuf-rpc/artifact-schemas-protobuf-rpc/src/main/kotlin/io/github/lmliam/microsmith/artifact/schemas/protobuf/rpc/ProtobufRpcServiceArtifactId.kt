package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import kotlin.reflect.KClass

public data class ProtobufRpcServiceArtifactId(public val packageName: String?, public val serviceName: String) :
    ArtifactId<ProtobufRpcServiceArtifact> {
    override val artifactType: KClass<ProtobufRpcServiceArtifact> = ProtobufRpcServiceArtifact::class

    public val protoFile: ProtoFileArtifactId = ProtoFileArtifactId(packageName, serviceName)
}
