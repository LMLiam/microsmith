package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class ProtobufRpcResolutionDiagnosticMapperTests :
    StringSpec(
        {
            "maps protobuf rpc issues to diagnostics" {
                val mapper = ProtobufRpcResolutionDiagnosticMapper()

                listOf(
                        ProtobufRpcResolutionIssue.SchemaIssue(
                            ProtobufResolutionIssue.SchemaDeclarationNameMismatch(
                                schemaName = "acme.user.v1.OtherService",
                                declarationName = "UserService",
                            ),
                        ),
                        ProtobufRpcResolutionIssue.EndpointMustTargetMessage(
                            serviceName = "UserService",
                            rpcName = "GetUser",
                            position = ProtobufRpcResolutionIssue.EndpointPosition.RESPONSE,
                            targetName = "Status",
                        ),
                        ProtobufRpcResolutionIssue.SchemaIssue(
                            ProtobufResolutionIssue.InvalidSchemaName(
                                schemaName = ".UserService",
                                reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.LEADING_DOT,
                            ),
                        ),
                        ProtobufRpcResolutionIssue.InvalidRpcName(serviceName = "UserService", rpcName = "1bad"),
                        ProtobufRpcResolutionIssue.DuplicateRpcName(serviceName = "UserService", rpcName = "GetUser"),
                        ProtobufRpcResolutionIssue.InvalidEndpointTargetName(
                            serviceName = "UserService",
                            rpcName = "GetUser",
                            position = ProtobufRpcResolutionIssue.EndpointPosition.REQUEST,
                            targetName = "acme..Request",
                            reason = ProtobufResolutionIssue.InvalidSchemaName.Reason.EMPTY_SEGMENT,
                        ),
                    )
                    .map(mapper::map) shouldContainExactly
                    listOf(
                        ResolutionDiagnostic(
                            code = "protobuf.schema-declaration-name-mismatch",
                            message =
                                "Protobuf schema 'acme.user.v1.OtherService' declares 'UserService', " +
                                    "but the schema identity and declaration name must match.",
                        ),
                        ResolutionDiagnostic(
                            code = "protobuf.rpc.endpoint-not-message",
                            message = "RPC 'GetUser' response must target a protobuf message, but was 'Status'.",
                        ),
                        ResolutionDiagnostic(
                            code = "protobuf.invalid-schema-name",
                            message = "Protobuf schema name '.UserService' is invalid: it starts with a leading dot.",
                        ),
                        ResolutionDiagnostic(
                            code = "protobuf.rpc.invalid-rpc-name",
                            message = "RPC name '1bad' of service 'UserService' is not a valid protobuf identifier.",
                        ),
                        ResolutionDiagnostic(
                            code = "protobuf.rpc.duplicate-rpc-name",
                            message = "Service 'UserService' declares RPC 'GetUser' more than once.",
                        ),
                        ResolutionDiagnostic(
                            code = "protobuf.rpc.invalid-endpoint-target-name",
                            message =
                                "RPC 'GetUser' request references invalid protobuf type name 'acme..Request': " +
                                    "it contains an empty segment.",
                        ),
                    )
            }
        },
    )
