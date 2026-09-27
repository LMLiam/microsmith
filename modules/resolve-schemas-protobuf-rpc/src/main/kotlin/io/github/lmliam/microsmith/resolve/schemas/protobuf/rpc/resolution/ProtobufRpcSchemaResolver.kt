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
import io.github.lmliam.microsmith.resolve.schemas.protobuf.resolveProtobufReferenceName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpc
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema
import kotlin.collections.sorted

internal class ProtobufRpcSchemaResolver {
    fun resolve(
        schema: ProtobufSchema,
        service: Service,
        schemasByName: Map<String, ProtobufSchema>,
    ): EitherNel<ProtobufRpcResolutionIssue, ResolvedProtobufRpcSchema> {
        val qualifiedName = QualifiedSchemaName.parse(schema.name)

        if (qualifiedName.typeName != service.name) {
            return ProtobufRpcResolutionIssue.SchemaDeclarationNameMismatch(
                    schemaName = schema.name,
                    declarationName = service.name,
                )
                .leftNel()
        }

        ProtobufNameValidation.requireIdentifier(service.name, "Service name")

        return service.rpcs
            .mapOrAccumulate { rpc ->
                resolveRpc(
                        service = service,
                        rpc = rpc,
                        current = qualifiedName,
                        schemasByName = schemasByName,
                    )
                    .bind()
            }
            .map { resolvedRpcs ->
                ResolvedProtobufRpcSchema(
                    qualifiedName = qualifiedName,
                    imports =
                        resolvedRpcs
                            .flatMap { rpc ->
                                listOfNotNull(
                                    rpc.request.importPath(qualifiedName),
                                    rpc.response.importPath(qualifiedName),
                                )
                            }
                            .distinct()
                            .sorted(),
                    rpcs = resolvedRpcs,
                )
            }
    }

    private fun resolveRpc(
        service: Service,
        rpc: Rpc,
        current: QualifiedSchemaName,
        schemasByName: Map<String, ProtobufSchema>,
    ): Either<ProtobufRpcResolutionIssue, ResolvedProtobufRpc> = either {
        ProtobufNameValidation.requireIdentifier(rpc.name, "RPC name")

        val request =
            resolveEndpoint(
                    service = service,
                    rpc = rpc,
                    endpoint = rpc.request,
                    current = current,
                    position = ProtobufRpcResolutionIssue.EndpointPosition.REQUEST,
                    schemasByName = schemasByName,
                )
                .bind()

        val response =
            resolveEndpoint(
                    service = service,
                    rpc = rpc,
                    endpoint = rpc.response,
                    current = current,
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

    private fun resolveEndpoint(
        service: Service,
        rpc: Rpc,
        endpoint: RpcEndpoint,
        current: QualifiedSchemaName,
        position: ProtobufRpcResolutionIssue.EndpointPosition,
        schemasByName: Map<String, ProtobufSchema>,
    ): Either<ProtobufRpcResolutionIssue, ResolvedProtobufRpcEndpoint> = either {
        val targetName =
            resolveProtobufReferenceName(
                endpoint.reference.qualifiedName,
                current,
            )

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

private fun ResolvedProtobufRpcEndpoint.importPath(current: QualifiedSchemaName): String? =
    qualifiedTypeName.takeUnless { it == current.fullyQualifiedName }?.replace('.', '/')?.plus(".proto")
