package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.core.microsmith
import io.github.lmliam.microsmith.dsl.helpers.require
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.core.schemas
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ProtobufRpcSchemasResolverTests :
    StringSpec({
        val resolver = ProtobufRpcSchemasResolver()

        "resolves rpc schemas into finalized rpc models" {
            val schemas =
                microsmith {
                    schemas {
                        protobuf {
                            "acme.user.v1" {
                                message("GetUserRequest")
                                message("GetUserResponse")
                                service("UserService") {
                                    "GetUser" { "GetUserRequest" to "GetUserResponse" }
                                }
                            }
                        }
                    }
                }
                    .require<SchemasExtension>()

            val resolved = resolver.resolve(schemas)
            requireNotNull(resolved).schemas.single().also { schema ->
                schema.qualifiedName.fullyQualifiedName shouldBe "acme.user.v1.UserService"
                schema.imports shouldContainExactly
                    listOf(
                        "acme/user/v1/GetUserRequest.proto",
                        "acme/user/v1/GetUserResponse.proto",
                    )
                schema.rpcs.single().also { rpc ->
                    rpc.name shouldBe "GetUser"
                    rpc.request.qualifiedTypeName shouldBe "acme.user.v1.GetUserRequest"
                    rpc.response.qualifiedTypeName shouldBe "acme.user.v1.GetUserResponse"
                }
            }
        }

        "rejects rpc endpoints that do not target protobuf messages" {
            val schemas =
                microsmith {
                    schemas {
                        protobuf {
                            enum("Status") {
                                value("UNKNOWN") { index(1) }
                            }
                            message("GetUserRequest")
                            service("UserService") {
                                "GetUser" { "GetUserRequest" to "Status" }
                            }
                        }
                    }
                }
                    .require<SchemasExtension>()

            val error = shouldThrow<IllegalArgumentException> {
                resolver.resolve(schemas)
            }

            error.message shouldBe "RPC 'GetUser' response must target a protobuf message, but was 'Status'."
        }

        "rejects duplicate rpc names during DSL authoring" {
            val error = shouldThrow<IllegalArgumentException> {
                microsmith {
                    schemas {
                        protobuf {
                            message("GetUserRequest")
                            message("GetUserResponse")
                            message("GetUserDetailsRequest")
                            message("GetUserDetailsResponse")
                            service("UserService") {
                                "GetUser" { "GetUserRequest" to "GetUserResponse" }
                                "GetUser" { "GetUserDetailsRequest" to "GetUserDetailsResponse" }
                            }
                        }
                    }
                }
            }

            error.message shouldBe "Duplicate RPC name: GetUser"
        }
    })
