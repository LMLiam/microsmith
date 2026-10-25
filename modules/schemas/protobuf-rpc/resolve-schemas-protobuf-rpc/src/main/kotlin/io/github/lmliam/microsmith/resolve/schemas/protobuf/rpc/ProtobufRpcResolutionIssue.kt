package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.ResolutionIssue

sealed interface ProtobufRpcResolutionIssue : ResolutionIssue {
    data class SchemaDeclarationNameMismatch(val schemaName: String, val declarationName: String) :
        ProtobufRpcResolutionIssue

    data class EndpointMustTargetMessage(
        val serviceName: String,
        val rpcName: String,
        val position: EndpointPosition,
        val targetName: String,
    ) : ProtobufRpcResolutionIssue

    data class EndpointTargetNotFound(
        val serviceName: String,
        val rpcName: String,
        val position: EndpointPosition,
        val targetName: String,
    ) : ProtobufRpcResolutionIssue

    enum class EndpointPosition {
        REQUEST,
        RESPONSE,
    }
}
