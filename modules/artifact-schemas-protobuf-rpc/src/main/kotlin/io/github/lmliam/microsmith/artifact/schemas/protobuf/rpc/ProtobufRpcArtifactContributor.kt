package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel

@ServiceProvider(ArtifactContributor::class)
class ProtobufRpcArtifactContributor : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun contribute(model: ResolvedProtobufRpcSchemaModel): List<ArtifactContribution<*>> =
        model.schemas.map { schema ->
            ProtobufRpcServiceContribution(
                artifactId = ProtobufRpcServiceArtifactId(
                    packageName = schema.qualifiedName.packageName,
                    serviceName = schema.qualifiedName.typeName,
                ),
                imports = schema.imports,
                operations = schema.rpcs.map { rpc ->
                    ProtobufRpcOperation(
                        name = rpc.name,
                        request = ProtobufRpcEndpoint(
                            typeName = rpc.request.qualifiedTypeName,
                            streaming = rpc.request.streaming,
                        ),
                        response = ProtobufRpcEndpoint(
                            typeName = rpc.response.qualifiedTypeName,
                            streaming = rpc.response.streaming,
                        ),
                    )
                },
            )
        }
}
