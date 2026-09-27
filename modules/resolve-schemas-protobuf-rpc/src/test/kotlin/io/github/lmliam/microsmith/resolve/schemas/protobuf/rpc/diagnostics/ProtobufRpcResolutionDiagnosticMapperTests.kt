package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class ProtobufRpcResolutionDiagnosticMapperTests :
    StringSpec({
        "maps protobuf rpc issues to diagnostics" {
            val mapper = ProtobufRpcResolutionDiagnosticMapper()

            listOf(
                    ProtobufRpcResolutionIssue.SchemaDeclarationNameMismatch(
                        schemaName = "acme.user.v1.OtherService",
                        declarationName = "UserService",
                    ),
                    ProtobufRpcResolutionIssue.EndpointMustTargetMessage(
                        serviceName = "UserService",
                        rpcName = "GetUser",
                        position = ProtobufRpcResolutionIssue.EndpointPosition.RESPONSE,
                        targetName = "Status",
                    ),
                )
                .map(mapper::map) shouldContainExactly
                listOf(
                    ResolutionDiagnostic(
                        code = "protobuf.rpc.schema-declaration-name-mismatch",
                        message = "Schema name 'acme.user.v1.OtherService' must match declaration name 'UserService'.",
                    ),
                    ResolutionDiagnostic(
                        code = "protobuf.rpc.endpoint-not-message",
                        message = "RPC 'GetUser' response must target a protobuf message, but was 'Status'.",
                    ),
                )
        }
    })
