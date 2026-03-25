package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import kotlin.reflect.KClass

data class ProtobufRpcServiceArtifactId(
    val packageName: String?,
    val serviceName: String,
) : ArtifactId<ProtobufRpcServiceArtifact> {
    override val artifactType: KClass<ProtobufRpcServiceArtifact> = ProtobufRpcServiceArtifact::class

    val fullyQualifiedName: String = packageName?.let { "$it.$serviceName" } ?: serviceName
}
