package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc

import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcOperation
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifactId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

class ProtobufServiceRendererTests :
    StringSpec({
        "renders unary and streaming routes" {
            val rendered =
                ProtobufServiceRenderer.render(
                    ProtobufRpcServiceArtifact(
                        id = ProtobufRpcServiceArtifactId(packageName = "pkg", serviceName = "UserService"),
                        imports = emptyList(),
                        operations = listOf(
                            ProtobufRpcOperation(
                                name = "GetUser",
                                requestTypeName = "pkg.GetUserRequest",
                                requestStreaming = false,
                                responseTypeName = "pkg.GetUserResponse",
                                responseStreaming = false,
                            ),
                            ProtobufRpcOperation(
                                name = "ChatUsers",
                                requestTypeName = "pkg.ChatRequest",
                                requestStreaming = true,
                                responseTypeName = "pkg.ChatResponse",
                                responseStreaming = true,
                            ),
                        ),
                    ),
                )

            rendered.shouldContain("rpc GetUser (pkg.GetUserRequest) returns (pkg.GetUserResponse);")
            rendered.shouldContain("rpc ChatUsers (stream pkg.ChatRequest) returns (stream pkg.ChatResponse);")
        }
    })
