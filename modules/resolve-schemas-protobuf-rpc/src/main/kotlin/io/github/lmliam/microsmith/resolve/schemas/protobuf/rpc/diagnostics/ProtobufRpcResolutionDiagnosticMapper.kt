package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.diagnostics

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue

@ServiceProvider(ResolutionIssueDiagnosticMapper::class)
class ProtobufRpcResolutionDiagnosticMapper : ResolutionIssueDiagnosticMapper<ProtobufRpcResolutionIssue> {
    override val issueType = ProtobufRpcResolutionIssue::class

    override fun map(issue: ProtobufRpcResolutionIssue): ResolutionDiagnostic =
        when (issue) {
            is ProtobufRpcResolutionIssue.SchemaDeclarationNameMismatch ->
                ResolutionDiagnostic(
                    code = "protobuf.rpc.schema-declaration-name-mismatch",
                    message =
                        "Schema name '${issue.schemaName}' must match declaration " +
                            "name '${issue.declarationName}'.",
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
