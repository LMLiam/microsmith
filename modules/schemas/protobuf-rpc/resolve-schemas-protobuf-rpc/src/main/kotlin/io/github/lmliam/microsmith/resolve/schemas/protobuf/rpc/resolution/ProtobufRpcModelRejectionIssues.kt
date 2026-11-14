package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.resolution

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel

internal fun ResolvedProtobufRpcSchema.Rejection.toIssue(serviceName: String): ProtobufRpcResolutionIssue =
    when (this) {
        is ResolvedProtobufRpcSchema.Rejection.DuplicateRpcName ->
            ProtobufRpcResolutionIssue.DuplicateRpcName(serviceName, name.value)
    }

internal fun ResolvedProtobufRpcSchemaModel.Rejection.toIssue(): ProtobufRpcResolutionIssue =
    when (this) {
        is ResolvedProtobufRpcSchemaModel.Rejection.DuplicateService ->
            ProtobufRpcResolutionIssue.SchemaIssue(
                ProtobufResolutionIssue.SymbolNameCollision(
                    scope = identity.packageName.orEmpty(),
                    name = identity.typeName.value,
                    declarations =
                        List(occurrences) {
                            ProtobufResolutionIssue.SymbolDeclaration.Type(identity.fullyQualifiedName)
                        },
                ),
            )
    }
