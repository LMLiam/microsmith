package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Rpc
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.RpcEndpoint
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.EnumValue
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ServiceEmissionValidatorTests :
    StringSpec({
        val validator = ServiceDeclarationSupport()

        "validates service routes target messages" {
            val schema =
                ProtobufSchema(
                    "pkg.UserService",
                    Service(
                        "UserService",
                        listOf(
                            Rpc(
                                "GetUser",
                                RpcEndpoint(Reference("pkg.GetUserRequest", Message("GetUserRequest"))),
                                RpcEndpoint(Reference("pkg.GetUserResponse", Message("GetUserResponse"))),
                            ),
                        ),
                    ),
                )

            validator.validate(schema, QualifiedSchemaName.parse(schema.name))
        }

        "rejects non-message rpc response targets" {
            val schema =
                ProtobufSchema(
                    "pkg.UserService",
                    Service(
                        "UserService",
                        listOf(
                            Rpc(
                                "GetUser",
                                RpcEndpoint(Reference("pkg.GetUserRequest", Message("GetUserRequest"))),
                                RpcEndpoint(
                                    Reference(
                                        "pkg.Status",
                                        Enum("Status", listOf(EnumValue("UNSPECIFIED", 0))),
                                    ),
                                ),
                            ),
                        ),
                    ),
                )

            val error = shouldThrow<IllegalArgumentException> {
                validator.validate(schema, QualifiedSchemaName.parse(schema.name))
            }

            error.message shouldBe "RPC 'GetUser' response must target a protobuf message, but was 'pkg.Status'."
        }

        "rejects duplicate rpc names within a service" {
            val schema =
                ProtobufSchema(
                    "pkg.UserService",
                    Service(
                        "UserService",
                        listOf(
                            Rpc(
                                "GetUser",
                                RpcEndpoint(Reference("pkg.GetUserRequest", Message("GetUserRequest"))),
                                RpcEndpoint(Reference("pkg.GetUserResponse", Message("GetUserResponse"))),
                            ),
                            Rpc(
                                "GetUser",
                                RpcEndpoint(Reference("pkg.GetUserDetailsRequest", Message("GetUserDetailsRequest"))),
                                RpcEndpoint(Reference("pkg.GetUserDetailsResponse", Message("GetUserDetailsResponse"))),
                            ),
                        ),
                    ),
                )

            val error = shouldThrow<IllegalArgumentException> {
                validator.validate(schema, QualifiedSchemaName.parse(schema.name))
            }

            error.message shouldBe "Duplicate RPC name in service 'UserService': GetUser"
        }
    })
