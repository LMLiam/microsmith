package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.leftNel
import arrow.core.mapOrAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Rpc
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.RpcEndpoint
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoFilePaths
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufDeclarationKind
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution.ProtobufSymbolTable
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpc
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema

internal class ProtobufRpcSchemaResolver {
    fun resolve(
        schema: ProtobufSchema,
        service: Service,
        symbols: ProtobufSymbolTable,
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

        return with(qualifiedName) {
            service.rpcs
                .mapOrAccumulate { rpc -> resolveRpc(service = service, rpc = rpc, symbols = symbols).bindNel() }
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
        symbols: ProtobufSymbolTable,
    ): EitherNel<ProtobufRpcResolutionIssue, ResolvedProtobufRpc> =
        Either.zipOrAccumulate(
            ProtoIdentifier.parse(rpc.name).mapLeft {
                ProtobufRpcResolutionIssue.InvalidRpcName(service.name, rpc.name)
            },
            resolveEndpoint(
                service,
                rpc,
                rpc.request,
                ProtobufRpcResolutionIssue.EndpointPosition.REQUEST,
                symbols,
            ),
            resolveEndpoint(
                service,
                rpc,
                rpc.response,
                ProtobufRpcResolutionIssue.EndpointPosition.RESPONSE,
                symbols,
            ),
        ) { _, request, response ->
            ResolvedProtobufRpc(name = rpc.name, request = request, response = response)
        }

    context(identity: QualifiedSchemaName)
    private fun resolveEndpoint(
        service: Service,
        rpc: Rpc,
        endpoint: RpcEndpoint,
        position: ProtobufRpcResolutionIssue.EndpointPosition,
        symbols: ProtobufSymbolTable,
    ): Either<ProtobufRpcResolutionIssue, ResolvedProtobufRpcEndpoint> = either {
        val target =
            symbols
                .lookup(identity, endpoint.reference)
                .mapLeft { failure ->
                    when (failure) {
                        is ProtobufSymbolTable.LookupFailure.InvalidName ->
                            ProtobufRpcResolutionIssue.InvalidEndpointTargetName(
                                serviceName = service.name,
                                rpcName = rpc.name,
                                position = position,
                                targetName = failure.issue.schemaName,
                                reason = failure.issue.reason,
                            )

                        is ProtobufSymbolTable.LookupFailure.Unknown ->
                            ProtobufRpcResolutionIssue.EndpointTargetNotFound(
                                serviceName = service.name,
                                rpcName = rpc.name,
                                position = position,
                                targetName = failure.target.fullyQualifiedName,
                            )
                    }
                }
                .bind()

        ensure(target.kind == ProtobufDeclarationKind.MESSAGE) {
            ProtobufRpcResolutionIssue.EndpointMustTargetMessage(
                serviceName = service.name,
                rpcName = rpc.name,
                position = position,
                targetName = target.identity.fullyQualifiedName,
            )
        }

        ResolvedProtobufRpcEndpoint(
            target = target.identity,
            streaming = endpoint.streaming,
        )
    }
}

context(identity: QualifiedSchemaName)
private fun ResolvedProtobufRpcEndpoint.importPath(): String? =
    target.takeUnless { it == identity }?.let { ProtoFilePaths.importPath(it.fullyQualifiedName) }
