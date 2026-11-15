package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcPluginScope
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel

@Inject
@ContributesIntoSet(ProtobufRpcPluginScope::class, binding = binding<ArtifactContributor<*>>())
internal class ProtobufRpcArtifactContributor : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
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
