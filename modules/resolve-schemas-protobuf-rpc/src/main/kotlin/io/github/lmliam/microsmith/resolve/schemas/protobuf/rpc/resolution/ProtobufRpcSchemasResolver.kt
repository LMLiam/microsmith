package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.resolution

import arrow.core.mapOrAccumulate
import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel

@ServiceProvider(DomainResolver::class)
class ProtobufRpcSchemasResolver : DomainResolver<SchemasExtension, ResolvedProtobufRpcSchemaModel> {
    private val schemaResolver = ProtobufRpcSchemaResolver()
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun resolve(authoring: SchemasExtension): DomainResolution<ResolvedProtobufRpcSchemaModel> {
        val protobufSchemas = authoring.schemas.filterIsInstance<ProtobufSchema>()
        val schemasByName = protobufSchemas.associateBy(ProtobufSchema::name)

        val rpcSchemas = protobufSchemas.mapNotNull { schema ->
            val service = schema.schema as? Service ?: return@mapNotNull null
            schema to service
        }

        if (rpcSchemas.isEmpty()) {
            return DomainResolution.NotApplicable
        }

        return rpcSchemas
            .mapOrAccumulate { (schema, service) ->
                schemaResolver.resolve(schema, service, schemasByName).bindNel()
            }
            .fold(
                ifLeft = { DomainResolution.Failure(it) },
                ifRight = { resolvedSchemas ->
                    DomainResolution.Success(
                        ResolvedProtobufRpcSchemaModel(
                            resolvedSchemas.sortedBy {
                                it.qualifiedName.fullyQualifiedName
                            },
                        ),
                    )
                },
            )
    }
}
