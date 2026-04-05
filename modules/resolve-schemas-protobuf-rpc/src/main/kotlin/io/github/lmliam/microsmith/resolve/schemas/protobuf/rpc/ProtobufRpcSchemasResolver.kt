package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Rpc
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.RpcEndpoint
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ServiceProvider(DomainResolver::class)
class ProtobufRpcSchemasResolver : DomainResolver<SchemasExtension, ResolvedProtobufRpcSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedProtobufRpcSchemaModel? {
        val schemas =
            authoring.schemas
                .filterIsInstance<ProtobufSchema>()
                .mapNotNull { schema ->
                    val service = schema.schema as? Service ?: return@mapNotNull null
                    val qualifiedName = QualifiedSchemaName.parse(schema.name)
                    resolve(schema, service, qualifiedName)
                }
                .sortedBy { it.qualifiedName.fullyQualifiedName }

        return schemas.takeIf(List<*>::isNotEmpty)?.let(::ResolvedProtobufRpcSchemaModel)
    }

    private fun resolve(
        schema: ProtobufSchema,
        service: Service,
        qualifiedName: QualifiedSchemaName,
    ): ResolvedProtobufRpcSchema {
        require(qualifiedName.typeName == service.name) {
            "Schema name '${schema.name}' must match declaration name '${service.name}'."
        }
        ProtobufNameValidation.requireIdentifier(service.name, "Service name")

        val rpcNames = mutableSetOf<String>()
        val resolvedRpcs =
            service.rpcs.map { rpc ->
                ProtobufNameValidation.requireIdentifier(rpc.name, "RPC name")
                require(rpcNames.add(rpc.name)) {
                    "Duplicate RPC name in service '${service.name}': ${rpc.name}"
                }
                ResolvedProtobufRpc(
                    name = rpc.name,
                    request = resolveEndpoint(rpc, rpc.request, qualifiedName, "request"),
                    response = resolveEndpoint(rpc, rpc.response, qualifiedName, "response"),
                )
            }

        val imports =
            resolvedRpcs
                .flatMap { listOfNotNull(it.request.importPath(qualifiedName), it.response.importPath(qualifiedName)) }
                .distinct()
                .sorted()

        return ResolvedProtobufRpcSchema(
            qualifiedName = qualifiedName,
            imports = imports,
            rpcs = resolvedRpcs,
        )
    }

    private fun resolveEndpoint(
        rpc: Rpc,
        endpoint: RpcEndpoint,
        current: QualifiedSchemaName,
        position: String,
    ): ResolvedProtobufRpcEndpoint {
        require(endpoint.reference.type is Message) {
            "RPC '${rpc.name}' $position must target a protobuf message, but was '${endpoint.reference.name}'."
        }

        return ResolvedProtobufRpcEndpoint(
            qualifiedTypeName = resolveQualifiedReferenceName(endpoint.reference.name, current),
            streaming = endpoint.streaming,
        )
    }

    private fun resolveQualifiedReferenceName(referenceName: String, current: QualifiedSchemaName): String {
        val normalized = ProtobufNameValidation.normalizeQualifiedName(referenceName, "RPC reference name")
        if ('.' in normalized) {
            return normalized
        }
        return current.packageName?.let { "$it.$normalized" } ?: normalized
    }
}

private fun ResolvedProtobufRpcEndpoint.importPath(current: QualifiedSchemaName): String? {
    return qualifiedTypeName
        .takeUnless { it == current.fullyQualifiedName }
        ?.replace('.', '/')
        ?.plus(".proto")
}
