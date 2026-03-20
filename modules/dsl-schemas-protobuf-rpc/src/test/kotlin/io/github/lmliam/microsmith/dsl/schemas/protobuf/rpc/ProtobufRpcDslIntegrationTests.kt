package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ProtobufRpcDslIntegrationTests :
    StringSpec({
        "protobuf RPC DSL builds service declarations with explicit and shorthand routes" {
            val schemas = SchemasBuilder().apply {
                protobuf {
                    message("GetUserRequest")
                    message("GetUserResponse")
                    message("WatchUsersRequest")
                    message("WatchUsersResponse")
                    message("DownloadUserRequest")
                    message("DownloadUserResponse")
                    message("UploadUserRequest")
                    message("UploadUserResponse")
                    service("UserService") {
                        "GetUser" {
                            request("GetUserRequest")
                            response("GetUserResponse")
                        }
                        "WatchUsers" {
                            request("WatchUsersRequest")
                            response("WatchUsersResponse") { stream() }
                        }
                        "ChatUsers" {
                            stream("WatchUsersRequest") to stream("WatchUsersResponse")
                        }
                        "DownloadUser" {
                            "DownloadUserRequest" to stream("DownloadUserResponse")
                        }
                        "UploadUser" {
                            stream("UploadUserRequest") to "UploadUserResponse"
                        }
                    }
                }
            }.toExtension().schemas.filterIsInstance<ProtobufSchema>()

            val service = schemas.first { it.name == "UserService" }.schema as Service

            service.rpcs.map(Rpc::name) shouldContainExactly listOf(
                "GetUser",
                "WatchUsers",
                "ChatUsers",
                "DownloadUser",
                "UploadUser",
            )
            service.rpcs[0].request.reference.type shouldBe Message("GetUserRequest")
            service.rpcs[0].response.reference.type shouldBe Message("GetUserResponse")
            service.rpcs[1].response.streaming shouldBe true
            service.rpcs[2].request.streaming shouldBe true
            service.rpcs[2].response.streaming shouldBe true
            service.rpcs[3].response.streaming shouldBe true
            service.rpcs[4].request.streaming shouldBe true
        }

        "protobuf RPC DSL rejects mixed explicit and shorthand route declarations" {
            val error =
                shouldThrowExactly<IllegalArgumentException> {
                    SchemasBuilder().apply {
                        protobuf {
                            message("GetUserRequest")
                            message("GetUserResponse")
                            service("UserService") {
                                "GetUser" {
                                    request("GetUserRequest")
                                    "GetUserRequest" to "GetUserResponse"
                                }
                            }
                        }
                    }.toExtension()
                }

            error.message shouldContain "cannot mix explicit request/response declarations with pair shorthand"
        }
    })
