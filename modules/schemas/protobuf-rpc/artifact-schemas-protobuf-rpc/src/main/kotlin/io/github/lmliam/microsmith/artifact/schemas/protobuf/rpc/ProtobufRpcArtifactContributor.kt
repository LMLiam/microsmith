package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel

@ServiceProvider(ArtifactContributor::class)
class ProtobufRpcArtifactContributor : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun contribute(model: ResolvedProtobufRpcSchemaModel): List<ArtifactContribution<*>> =
        model.schemas.map { schema ->
            ProtobufRpcServiceContribution(
                artifactId =
                    ProtobufRpcServiceArtifactId(
                        packageName = schema.qualifiedName.packageName,
                        serviceName = schema.qualifiedName.typeName.value,
                    ),
                imports = schema.dependencies.map { ProtoFileArtifactId.of(it).importPath },
                operations = ProtobufRpcOperationMapper.mapList(schema.rpcs),
            )
        }
}
