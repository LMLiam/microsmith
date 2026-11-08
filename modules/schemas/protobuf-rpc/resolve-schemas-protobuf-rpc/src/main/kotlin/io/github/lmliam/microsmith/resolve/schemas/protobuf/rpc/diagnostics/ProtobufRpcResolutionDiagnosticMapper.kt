package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.diagnostics

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
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

            is ProtobufRpcResolutionIssue.InvalidSchemaName ->
                ResolutionDiagnostic(
                    code = "protobuf.rpc.invalid-schema-name",
                    message = "Schema name '${issue.schemaName}' is invalid: it ${issue.reason.displayName()}.",
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

    private fun ProtobufResolutionIssue.InvalidSchemaName.Reason.displayName(): String =
        when (this) {
            ProtobufResolutionIssue.InvalidSchemaName.Reason.BLANK -> "is blank"

            ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT -> "starts with a leading dot"

            ProtobufResolutionIssue.InvalidSchemaName.Reason.TRAILING_DOT -> "ends with a trailing dot"

            ProtobufResolutionIssue.InvalidSchemaName.Reason.WHITESPACE -> "contains whitespace"

            ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT -> "contains an empty segment"

            ProtobufResolutionIssue.InvalidSchemaName.Reason.INVALID_IDENTIFIER_SEGMENT ->
                "contains an invalid identifier segment"
        }
}
