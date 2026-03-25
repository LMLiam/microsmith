package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileContribution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import kotlin.reflect.KClass

@ServiceProvider(ArtifactContributor::class)
class ProtobufRpcArtifactContributor : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
    override val resolvedType: KClass<ResolvedProtobufRpcSchemaModel> = ResolvedProtobufRpcSchemaModel::class

    override fun contribute(model: ResolvedProtobufRpcSchemaModel): List<ArtifactContribution<*>> {
        return model.schemas.map { schema ->
            ProtoFileContribution(
                artifactId = ProtoFileArtifactId(relativePath = schema.qualifiedName.relativePath()),
                packageName = schema.qualifiedName.packageName,
                imports = schema.imports,
                declarations = listOf(
                    ProtoDeclaration(
                        name = schema.qualifiedName.typeName,
                        contents = ProtobufServiceRenderer.render(schema),
                    ),
                ),
            )
        }
    }
}
