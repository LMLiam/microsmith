package io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.render.ProtobufDeclarationRenderer
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufPluginScope
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

@Inject
@ContributesIntoSet(ProtobufPluginScope::class, binding = binding<ArtifactContributor<*>>())
internal class ProtobufArtifactContributor(private val declarations: ProtobufDeclarationRenderer) :
    ArtifactContributor<ResolvedProtobufSchemaModel> {
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
