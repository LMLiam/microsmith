package io.github.lmliam.microsmith.lower.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.lower.core.ArtifactLowerer
import kotlin.reflect.KClass

@ServiceProvider(ArtifactLowerer::class)
class ProtobufRpcServiceArtifactLowerer : ArtifactLowerer<ProtobufRpcServiceArtifact> {
    override val artifactType: KClass<ProtobufRpcServiceArtifact> = ProtobufRpcServiceArtifact::class

    override fun lower(artifact: ProtobufRpcServiceArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(
            ProtoFileContribution(
                artifactId = ProtoFileArtifactId(
                    packageName = artifact.id.packageName,
                    typeName = artifact.id.serviceName,
                ),
                packageName = artifact.id.packageName,
                imports = artifact.imports,
                declarations = listOf(
                    ProtoDeclaration(
                        name = artifact.id.serviceName,
                        contents = ProtobufServiceRenderer.render(artifact),
                    ),
                ),
            ),
        )
    }
}
