package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class ProtobufRpcServiceArtifactAssemblerTests :
    StringSpec({
        val assembler = ProtobufRpcServiceArtifactAssembler()
        val sharedId = ProtobufRpcServiceArtifactId(packageName = "acme.user.v1", serviceName = "UserService")

        fun operation(name: String, request: String = "acme.user.v1.GetUserRequest") =
            ProtobufRpcOperation(
                name = name,
                request = ProtobufRpcEndpoint(typeName = request, streaming = false),
                response = ProtobufRpcEndpoint(typeName = "acme.user.v1.GetUserResponse", streaming = false),
            )

        "merges imports and operations for the same service" {
            val assembled =
                assembler.merge(
                    assembler.create(
                        ProtobufRpcServiceContribution(
                            artifactId = sharedId,
                            imports = listOf("acme/user/v1/Role.proto"),
                            operations = listOf(operation("GetUser")),
                        )
                    ),
                    ProtobufRpcServiceContribution(
                        artifactId = sharedId,
                        imports = listOf("acme/user/v1/Profile.proto", "acme/user/v1/Role.proto"),
                        operations = listOf(operation("ListUsers")),
                    ),
                )

            assembled.imports shouldContainExactly listOf("acme/user/v1/Profile.proto", "acme/user/v1/Role.proto")
            assembled.operations.map(ProtobufRpcOperation::name) shouldContainExactly listOf("GetUser", "ListUsers")
        }

        "rejects conflicting operations with the same name" {
            val current =
                assembler.create(
                    ProtobufRpcServiceContribution(artifactId = sharedId, operations = listOf(operation("GetUser")))
                )

            val error =
                shouldThrow<IllegalArgumentException> {
                    assembler.merge(
                        current,
                        ProtobufRpcServiceContribution(
                            artifactId = sharedId,
                            operations = listOf(operation("GetUser", request = "acme.user.v1.Other")),
                        ),
                    )
                }

            error.message shouldBe "Conflicting protobuf RPC 'GetUser' for 'acme.user.v1.UserService'."
        }
    })
