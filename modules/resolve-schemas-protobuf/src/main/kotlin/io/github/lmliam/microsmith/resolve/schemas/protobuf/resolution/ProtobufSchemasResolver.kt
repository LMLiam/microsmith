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
        val schemas = protobufSchemas.filter { schema ->
            when (schema.schema) {
                is Message, is Enum -> true
                else -> false
            }
        }.sortedBy(ProtobufSchema::name)

        if (schemas.isEmpty()) {
            return DomainResolution.NotApplicable
        }

        val symbolTable = ProtobufSymbolTable(protobufSchemas)
        val resolver = ProtobufDeclarationResolver(ProtobufReferenceResolver(symbolTable))
        val issues = mutableListOf<ProtobufResolutionIssue>()
        val resolved = schemas.mapNotNull { schema ->
            resolver.resolve(schema).fold(
                ifLeft = { failures ->
                    issues.addAll(failures)
                    null
                },
                ifRight = { it },
            )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            DomainResolution.Failure(accumulatedIssues)
        } else {
            DomainResolution.Success(ResolvedProtobufSchemaModel(resolved))
        }
    }
}
