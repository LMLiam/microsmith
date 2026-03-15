package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ProtobufRpcDslIntegrationTests :
    StringSpec({
        "protobuf RPC DSL builds service declarations with explicit and shorthand routes" {
            val schemas = SchemasBuilder().apply {
                protobuf {
                    message("GetUserRequest")
                    message("GetUserResponse")
                    message("WatchUsersRequest")
                    message("WatchUsersResponse")
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
                    }
                }
            }.toExtension().schemas.filterIsInstance<ProtobufSchema>()

            val service = schemas.first { it.name == "UserService" }.schema as Service

            service.rpcs.map(Rpc::name) shouldContainExactly listOf("GetUser", "WatchUsers", "ChatUsers")
            service.rpcs[0].request.reference.type shouldBe Message("GetUserRequest")
            service.rpcs[0].response.reference.type shouldBe Message("GetUserResponse")
            service.rpcs[1].response.streaming shouldBe true
            service.rpcs[2].request.streaming shouldBe true
            service.rpcs[2].response.streaming shouldBe true
        }
    })
