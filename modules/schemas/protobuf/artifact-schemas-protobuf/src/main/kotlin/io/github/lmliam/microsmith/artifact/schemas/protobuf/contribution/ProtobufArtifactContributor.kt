package io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.render.ProtobufDeclarationRenderer
import io.github.lmliam.microsmith.artifact.schemas.protobuf.render.ProtobufFieldRenderer
import io.github.lmliam.microsmith.artifact.schemas.protobuf.render.ProtobufReservedSectionRenderer
import io.github.lmliam.microsmith.artifact.schemas.protobuf.render.ProtobufValueTypeRenderer
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

@ServiceProvider(ArtifactContributor::class)
class ProtobufArtifactContributor private constructor(private val declarations: ProtobufDeclarationRenderer) :
    ArtifactContributor<ResolvedProtobufSchemaModel> {
    constructor() :
        this(
            ProtobufDeclarationRenderer(
                ProtobufReservedSectionRenderer(),
                ProtobufFieldRenderer(ProtobufValueTypeRenderer()),
            ),
        )

    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun contribute(model: ResolvedProtobufSchemaModel): List<ArtifactContribution<*>> =
        model.schemas.map { schema ->
            ProtoFileContribution(
                ProtoFileArtifactId.of(schema.identity),
                schema.dependencies.map { ProtoFileArtifactId.of(it).importPath },
                listOf(ProtoDeclaration(schema.identity.typeName.value, declarations.render(schema.declaration))),
            )
        }
}
