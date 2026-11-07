package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.toNonEmptyListOrNull
import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

@ServiceProvider(DomainResolver::class)
class ProtobufSchemasResolver : DomainResolver<SchemasExtension, ResolvedProtobufSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun resolve(authoring: SchemasExtension): DomainResolution<ResolvedProtobufSchemaModel> {
        val protobufSchemas = authoring.schemas.filterIsInstance<ProtobufSchema>()
        val schemas = protobufSchemas.sortedBy(ProtobufSchema::name)

        val symbolTable = ProtobufSymbolTable(protobufSchemas)
        val resolver = ProtobufDeclarationResolver(ProtobufReferenceResolver(symbolTable))
        val issues = mutableListOf<ProtobufResolutionIssue>()
        val resolved = schemas.mapNotNull { schema ->
            val resolution =
                when (val declaration = schema.schema) {
                    is Message -> resolver.resolveMessage(schema, declaration)
                    is Enum -> resolver.resolveEnum(schema, declaration)
                    else -> null
                }

            resolution?.fold(
                ifLeft = { failures ->
                    issues.addAll(failures)
                    null
                },
                ifRight = { it },
            )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return when {
            accumulatedIssues != null -> DomainResolution.Failure(accumulatedIssues)
            resolved.isEmpty() -> DomainResolution.NotApplicable
            else -> DomainResolution.Success(ResolvedProtobufSchemaModel(resolved))
        }
    }
}
