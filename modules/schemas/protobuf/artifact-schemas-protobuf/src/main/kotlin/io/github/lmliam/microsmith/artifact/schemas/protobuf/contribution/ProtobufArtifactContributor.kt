package io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.renderProtobufDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

@ServiceProvider(ArtifactContributor::class)
class ProtobufArtifactContributor : ArtifactContributor<ResolvedProtobufSchemaModel> {
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun contribute(model: ResolvedProtobufSchemaModel): List<ArtifactContribution<*>> =
        model.schemas.map { schema ->
            ProtoFileContribution(
                ProtoFileArtifactId.of(schema.identity),
                schema.dependencies.map { ProtoFileArtifactId.of(it).importPath },
                listOf(ProtoDeclaration(schema.identity.typeName.value, renderProtobufDeclaration(schema.declaration))),
            )
        }
}
