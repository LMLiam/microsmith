package io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.renderProtobufDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.validateProtobufDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifactId
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ServiceProvider(ArtifactContributor::class)
class ProtobufArtifactContributor : ArtifactContributor<ResolvedProtobufSchemaModel> {
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun contribute(model: ResolvedProtobufSchemaModel): List<ArtifactContribution<*>> =
        model.schemas.map { schema ->
            validateProtobufDeclaration(schema.declaration)

            ProtoFileContribution(
                ProtoFileArtifactId(schema.identity.packageName, schema.identity.typeName),
                schema.identity.packageName,
                schema.dependencies.map(QualifiedSchemaName::toImportPath),
                listOf(ProtoDeclaration(schema.identity.typeName, renderProtobufDeclaration(schema.declaration))),
                setOf("schemas.protobuf.${schema.identity.fullyQualifiedName}"),
            )
        }
}

private fun QualifiedSchemaName.toImportPath(): String = buildString {
    packageName?.let {
        append(it.replace('.', '/'))
        append('/')
    }
    append(typeName)
    append(".proto")
}
