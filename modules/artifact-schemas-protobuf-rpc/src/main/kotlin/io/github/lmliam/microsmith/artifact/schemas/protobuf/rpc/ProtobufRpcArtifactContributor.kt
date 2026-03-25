package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import kotlin.reflect.KClass

@ServiceProvider(ArtifactContributor::class)
class ProtobufRpcArtifactContributor : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
    override val resolvedType: KClass<ResolvedProtobufRpcSchemaModel> = ResolvedProtobufRpcSchemaModel::class

    override fun contribute(model: ResolvedProtobufRpcSchemaModel): List<ArtifactContribution<*>> {
        return model.schemas.map { schema ->
            ProtobufRpcServiceContribution(
                artifactId = ProtobufRpcServiceArtifactId(
                    packageName = schema.qualifiedName.packageName,
                    serviceName = schema.qualifiedName.typeName,
                ),
                imports = schema.imports,
                operations = schema.rpcs.map { rpc ->
                    ProtobufRpcOperation(
                        name = rpc.name,
                        requestTypeName = rpc.request.qualifiedTypeName,
                        requestStreaming = rpc.request.streaming,
                        responseTypeName = rpc.response.qualifiedTypeName,
                        responseStreaming = rpc.response.streaming,
                    )
                },
            )
        }
    }
}
