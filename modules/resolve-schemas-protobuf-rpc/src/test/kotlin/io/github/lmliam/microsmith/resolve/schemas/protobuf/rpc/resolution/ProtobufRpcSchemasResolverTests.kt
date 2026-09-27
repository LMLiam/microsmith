package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.resolution

import io.github.lmliam.microsmith.dsl.microsmith
import io.github.lmliam.microsmith.dsl.require
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.github.lmliam.microsmith.dsl.schemas.schemas
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue.EndpointMustTargetMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue.EndpointPosition
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcResolutionIssue.SchemaDeclarationNameMismatch
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf

class ProtobufRpcSchemasResolverTests :
    StringSpec({
        val resolver = ProtobufRpcSchemasResolver()

        "resolves rpc schemas into finalized rpc models" {
            val schemas = microsmith {
                schemas {
                    protobuf {
                        "acme.user.v1" {
                            message("GetUserRequest")
                            message("GetUserResponse")

                            service("UserService") { "GetUser" { "GetUserRequest" to "GetUserResponse" } }
                        }
                    }
                }
            }
                .require<SchemasExtension>()

            val resolved =
                resolver
                    .resolve(schemas)
                    .shouldBeTypeOf<DomainResolution.Success<ResolvedProtobufRpcSchemaModel>>()
                    .model

            resolved.schemas.single().also { schema ->
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

        "returns a typed issue when an rpc endpoint does not target a protobuf message" {
            val schemas = microsmith {
                schemas {
                    protobuf {
                        enum("Status") { value("UNKNOWN") { index(1) } }

                        message("GetUserRequest")

                        service("UserService") { "GetUser" { "GetUserRequest" to "Status" } }
                    }
                }
            }
                .require<SchemasExtension>()

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    EndpointMustTargetMessage(
                        serviceName = "UserService",
                        rpcName = "GetUser",
                        position = EndpointPosition.RESPONSE,
                        targetName = "Status",
                    )
                )
        }

        "returns a typed issue when schema and service names disagree" {
            val schemas =
                SchemasExtension(
                    setOf(
                        ProtobufSchema(
                            name = "acme.user.v1.OtherService",
                            schema =
                                Service(
                                    name = "UserService",
                                    rpcs = emptyList(),
                                ),
                        )
                    )
                )

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    SchemaDeclarationNameMismatch(
                        schemaName = "acme.user.v1.OtherService",
                        declarationName = "UserService",
                    )
                )
        }

        "returns not applicable when no protobuf rpc schemas exist" {
            val schemas = microsmith {
                schemas { protobuf { message("User") } }
            }
                .require<SchemasExtension>()

            resolver.resolve(schemas) shouldBe DomainResolution.NotApplicable
        }

        "rejects duplicate rpc names during DSL authoring" {
            val error =
                shouldThrow<IllegalArgumentException> {
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

        "returns a typed issue when an rpc endpoint target is missing" {
            val schemas = microsmith {
                schemas {
                    protobuf {
                        message("GetUserRequest")

                        service("UserService") { "GetUser" { "GetUserRequest" to "MissingResponse" } }
                    }
                }
            }
                .require<SchemasExtension>()

            val failure = resolver.resolve(schemas).shouldBeTypeOf<DomainResolution.Failure>()

            failure.issues.toList() shouldContainExactly
                listOf(
                    ProtobufRpcResolutionIssue.EndpointTargetNotFound(
                        serviceName = "UserService",
                        rpcName = "GetUser",
                        position = EndpointPosition.RESPONSE,
                        targetName = "MissingResponse",
                    )
                )
        }
    })
