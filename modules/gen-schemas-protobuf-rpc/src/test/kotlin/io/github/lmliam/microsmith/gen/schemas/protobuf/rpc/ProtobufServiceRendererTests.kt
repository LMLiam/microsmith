package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpc
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchema
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

class ProtobufServiceRendererTests :
    StringSpec({
        "renders unary and streaming routes" {
            val rendered =
                ProtobufServiceRenderer.render(
                    ResolvedProtobufRpcSchema(
                        qualifiedName = QualifiedSchemaName.parse("pkg.UserService"),
                        imports = emptyList(),
                        rpcs = listOf(
                            ResolvedProtobufRpc(
                                "GetUser",
                                ResolvedProtobufRpcEndpoint("pkg.GetUserRequest", streaming = false),
                                ResolvedProtobufRpcEndpoint("pkg.GetUserResponse", streaming = false),
                            ),
                            ResolvedProtobufRpc(
                                "ChatUsers",
                                ResolvedProtobufRpcEndpoint("pkg.ChatRequest", streaming = true),
                                ResolvedProtobufRpcEndpoint("pkg.ChatResponse", streaming = true),
                            ),
                        ),
                    ),
                )

            rendered.shouldContain("rpc GetUser (pkg.GetUserRequest) returns (pkg.GetUserResponse);")
            rendered.shouldContain("rpc ChatUsers (stream pkg.ChatRequest) returns (stream pkg.ChatResponse);")
        }
    })
