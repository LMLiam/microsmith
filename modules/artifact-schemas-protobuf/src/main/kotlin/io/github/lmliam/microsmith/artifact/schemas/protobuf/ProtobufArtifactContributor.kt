package io.github.lmliam.microsmith.artifact.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.ProtobufDeclarationHandlerRegistry
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

@ServiceProvider(ArtifactContributor::class)
class ProtobufArtifactContributor : ArtifactContributor<ResolvedProtobufSchemaModel> {
    private val declarationHandlerRegistry = ProtobufDeclarationHandlerRegistry()

    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun contribute(model: ResolvedProtobufSchemaModel): List<ArtifactContribution<*>> {
        return model.schemas.map { resolvedSchema ->
            val declarationHandler = declarationHandlerRegistry.resolve(resolvedSchema.schema.schema)
            declarationHandler.validate(resolvedSchema.schema, resolvedSchema.qualifiedName)

            ProtoFileContribution(
                artifactId = ProtoFileArtifactId(
                    packageName = resolvedSchema.qualifiedName.packageName,
                    typeName = resolvedSchema.qualifiedName.typeName,
                ),
                packageName = resolvedSchema.qualifiedName.packageName,
                imports = declarationHandler.collectImports(resolvedSchema.schema.schema, resolvedSchema.qualifiedName),
                declarations = listOf(
                    ProtoDeclaration(
                        name = resolvedSchema.qualifiedName.typeName,
                        contents = declarationHandler.render(resolvedSchema.schema.schema),
                    ),
                ),
            )
        }
    }
}
