package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.ResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

public sealed interface ProtobufRpcResolutionIssue : ResolutionIssue {
    public data class SchemaIssue(public val issue: ProtobufResolutionIssue) : ProtobufRpcResolutionIssue

    public data class InvalidEndpointTargetName(
        public val serviceName: String,
        public val rpcName: String,
        public val position: EndpointPosition,
        public val targetName: String,
        public val reason: ProtobufResolutionIssue.InvalidSchemaName.Reason,
    ) : ProtobufRpcResolutionIssue

    public data class EndpointMustTargetMessage(
        public val serviceName: String,
        public val rpcName: String,
        public val position: EndpointPosition,
        public val targetName: String,
    ) : ProtobufRpcResolutionIssue

    public data class EndpointTargetNotFound(
        public val serviceName: String,
        public val rpcName: String,
        public val position: EndpointPosition,
        public val targetName: String,
    ) : ProtobufRpcResolutionIssue

    public data class InvalidRpcName(public val serviceName: String, public val rpcName: String) :
        ProtobufRpcResolutionIssue

    public data class DuplicateRpcName(public val serviceName: String, public val rpcName: String) :
        ProtobufRpcResolutionIssue

    public enum class EndpointPosition {
        REQUEST,
        RESPONSE,
    }
}
