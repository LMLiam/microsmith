package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Rpc
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.RpcEndpoint
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

class ProtobufServiceRendererTests :
    StringSpec({
        "renders unary and streaming routes" {
            val rendered =
                ProtobufServiceRenderer.render(
                    Service(
                        "UserService",
                        listOf(
                            Rpc(
                                "GetUser",
                                RpcEndpoint(Reference("pkg.GetUserRequest")),
                                RpcEndpoint(Reference("pkg.GetUserResponse")),
                            ),
                            Rpc(
                                "ChatUsers",
                                RpcEndpoint(Reference("pkg.ChatRequest"), streaming = true),
                                RpcEndpoint(Reference("pkg.ChatResponse"), streaming = true),
                            ),
                        ),
                    ),
                )

            rendered.shouldContain("rpc GetUser (pkg.GetUserRequest) returns (pkg.GetUserResponse);")
            rendered.shouldContain("rpc ChatUsers (stream pkg.ChatRequest) returns (stream pkg.ChatResponse);")
        }
    })
