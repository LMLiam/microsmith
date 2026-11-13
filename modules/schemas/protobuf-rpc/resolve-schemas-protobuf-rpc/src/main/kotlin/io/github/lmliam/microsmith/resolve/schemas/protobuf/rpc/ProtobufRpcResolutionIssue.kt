package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.ResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

sealed interface ProtobufRpcResolutionIssue : ResolutionIssue {
    data class SchemaIssue(val issue: ProtobufResolutionIssue) : ProtobufRpcResolutionIssue

    data class InvalidEndpointTargetName(
        val serviceName: String,
        val rpcName: String,
        val position: EndpointPosition,
        val targetName: String,
        val reason: ProtobufResolutionIssue.InvalidSchemaName.Reason,
    ) : ProtobufRpcResolutionIssue

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

    data class InvalidRpcName(val serviceName: String, val rpcName: String) : ProtobufRpcResolutionIssue

    enum class EndpointPosition {
        REQUEST,
        RESPONSE,
    }
}
