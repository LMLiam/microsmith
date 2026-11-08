package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.leftNel
import arrow.core.mapOrAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.ensureNotNull
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Rpc
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.RpcEndpoint
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpc
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema

internal class ProtobufRpcSchemaResolver {
    fun resolve(
        schema: ProtobufSchema,
        service: Service,
        schemasByName: Map<String, ProtobufSchema>,
    ): EitherNel<ProtobufRpcResolutionIssue, ResolvedProtobufRpcSchema> {
        val qualifiedName =
            when (val parsed = QualifiedSchemaName.parse(schema.name)) {
                is Either.Left ->
                    return ProtobufRpcResolutionIssue.InvalidSchemaName(parsed.value.schemaName, parsed.value.reason)
                        .leftNel()

                is Either.Right -> parsed.value
            }

        if (qualifiedName.typeName.value != service.name) {
            return ProtobufRpcResolutionIssue.SchemaDeclarationNameMismatch(
                    schemaName = schema.name,
                    declarationName = service.name,
                )
                .leftNel()
        }

        ProtobufNameValidation.requireIdentifier(service.name, "Service name")

        return with(qualifiedName) {
            service.rpcs
                .mapOrAccumulate { rpc ->
                    resolveRpc(service = service, rpc = rpc, schemasByName = schemasByName).bind()
                }
                .map { resolvedRpcs ->
                    ResolvedProtobufRpcSchema(
                        qualifiedName = qualifiedName,
                        imports =
                            resolvedRpcs
                                .flatMap { rpc -> listOfNotNull(rpc.request.importPath(), rpc.response.importPath()) }
                                .distinct()
                                .sorted(),
                        rpcs = resolvedRpcs,
                    )
                }
        }
    }

    context(identity: QualifiedSchemaName)
    private fun resolveRpc(
        service: Service,
        rpc: Rpc,
        schemasByName: Map<String, ProtobufSchema>,
    ): Either<ProtobufRpcResolutionIssue, ResolvedProtobufRpc> = either {
        ProtobufNameValidation.requireIdentifier(rpc.name, "RPC name")

        val request =
            resolveEndpoint(
                    service = service,
                    rpc = rpc,
                    endpoint = rpc.request,
                    position = ProtobufRpcResolutionIssue.EndpointPosition.REQUEST,
                    schemasByName = schemasByName,
                )
                .bind()

        val response =
            resolveEndpoint(
                    service = service,
                    rpc = rpc,
                    endpoint = rpc.response,
                    position = ProtobufRpcResolutionIssue.EndpointPosition.RESPONSE,
                    schemasByName = schemasByName,
                )
                .bind()

        ResolvedProtobufRpc(
            name = rpc.name,
            request = request,
            response = response,
        )
    }

    context(identity: QualifiedSchemaName)
    private fun resolveEndpoint(
        service: Service,
        rpc: Rpc,
        endpoint: RpcEndpoint,
        position: ProtobufRpcResolutionIssue.EndpointPosition,
        schemasByName: Map<String, ProtobufSchema>,
    ): Either<ProtobufRpcResolutionIssue, ResolvedProtobufRpcEndpoint> = either {
        val targetName =
            identity
                .resolveReference(endpoint.reference)
                .map { it.fullyQualifiedName }
                .mapLeft { invalid ->
                    ProtobufRpcResolutionIssue.InvalidEndpointTargetName(
                        serviceName = service.name,
                        rpcName = rpc.name,
                        position = position,
                        targetName = invalid.schemaName,
                        reason = invalid.reason,
                    )
                }
                .bind()

        val target =
            ensureNotNull(schemasByName[targetName]) {
                ProtobufRpcResolutionIssue.EndpointTargetNotFound(
                    serviceName = service.name,
                    rpcName = rpc.name,
                    position = position,
                    targetName = targetName,
                )
            }

        ensure(target.schema is Message) {
            ProtobufRpcResolutionIssue.EndpointMustTargetMessage(
                serviceName = service.name,
                rpcName = rpc.name,
                position = position,
                targetName = targetName,
            )
        }

        ResolvedProtobufRpcEndpoint(
            qualifiedTypeName = targetName,
            streaming = endpoint.streaming,
        )
    }
}

context(identity: QualifiedSchemaName)
private fun ResolvedProtobufRpcEndpoint.importPath(): String? =
    qualifiedTypeName.takeUnless { it == identity.fullyQualifiedName }?.replace('.', '/')?.plus(".proto")
