package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.diagnostics

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics.ProtobufResolutionDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics.displayName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue

@ServiceProvider(ResolutionIssueDiagnosticMapper::class)
class ProtobufRpcResolutionDiagnosticMapper : ResolutionIssueDiagnosticMapper<ProtobufRpcResolutionIssue> {
    private val schemaIssueMapper = ProtobufResolutionDiagnosticMapper()
    override val issueType = ProtobufRpcResolutionIssue::class

    override fun map(issue: ProtobufRpcResolutionIssue): ResolutionDiagnostic =
        when (issue) {
            is ProtobufRpcResolutionIssue.SchemaIssue -> schemaIssueMapper.map(issue.issue)

            is ProtobufRpcResolutionIssue.InvalidRpcName ->
                ResolutionDiagnostic(
                    code = "protobuf.rpc.invalid-rpc-name",
                    message =
                        "RPC name '${issue.rpcName}' of service '${issue.serviceName}' " +
                            "is not a valid protobuf identifier.",
                )

            is ProtobufRpcResolutionIssue.InvalidEndpointTargetName ->
                ResolutionDiagnostic(
                    code = "protobuf.rpc.invalid-endpoint-target-name",
                    message =
                        "RPC '${issue.rpcName}' ${issue.position.displayName()} references invalid protobuf type " +
                            "name '${issue.targetName}': it ${issue.reason.displayName()}.",
                )

            is ProtobufRpcResolutionIssue.EndpointTargetNotFound ->
                ResolutionDiagnostic(
                    code = "protobuf.rpc.endpoint-target-not-found",
                    message =
                        "RPC '${issue.rpcName}' ${issue.position.displayName()} references unknown " +
                            "protobuf type '${issue.targetName}'.",
                )

            is ProtobufRpcResolutionIssue.EndpointMustTargetMessage ->
                ResolutionDiagnostic(
                    code = "protobuf.rpc.endpoint-not-message",
                    message =
                        "RPC '${issue.rpcName}' ${issue.position.displayName()} must target a protobuf message, " +
                            "but was '${issue.targetName}'.",
                )
        }

    private fun ProtobufRpcResolutionIssue.EndpointPosition.displayName(): String =
        when (this) {
            ProtobufRpcResolutionIssue.EndpointPosition.REQUEST -> "request"
            ProtobufRpcResolutionIssue.EndpointPosition.RESPONSE -> "response"
        }
}
