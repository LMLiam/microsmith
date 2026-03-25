package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc.integration

import io.github.lmliam.microsmith.dsl.core.microsmith
import io.github.lmliam.microsmith.dsl.schemas.core.schemas
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.github.lmliam.microsmith.gen.files.TemporaryDirectory
import io.github.lmliam.microsmith.gen.helpers.generateTo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import kotlin.io.use

class ProtobufRpcGenerationIntegrationTests :
    StringSpec({
        "generateTo emits rpc services with imported request and response messages" {
            val model =
                microsmith {
                    schemas {
                        protobuf {
                            message("GetUserRequest")
                            message("GetUserResponse")
                            service("UserService") {
                                "GetUser" { "GetUserRequest" to "GetUserResponse" }
                            }
                        }
                    }
                }

            TemporaryDirectory.create(prefix = "protobuf-rpc-generation-").use { outputSpace ->
                model.generateTo(outputSpace.root)
                val generatedFile = outputSpace.root.resolve("proto/UserService.proto")
                val contents = Files.readString(generatedFile)

                Files.exists(generatedFile) shouldBe true
                contents.shouldContain("import \"GetUserRequest.proto\";")
                contents.shouldContain("import \"GetUserResponse.proto\";")
                contents.shouldContain("service UserService {")
                contents.shouldContain("rpc GetUser (GetUserRequest) returns (GetUserResponse);")
            }
        }

        "generateTo emits qualified rpc services under the namespaced proto path" {
            val model =
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

            TemporaryDirectory.create(prefix = "protobuf-rpc-generation-").use { outputSpace ->
                model.generateTo(outputSpace.root)
                val generatedFile = outputSpace.root.resolve("proto/acme/user/v1/UserService.proto")
                val contents = Files.readString(generatedFile)

                Files.exists(generatedFile) shouldBe true
                contents.shouldContain("package acme.user.v1;")
                contents.shouldContain("import \"acme/user/v1/GetUserRequest.proto\";")
                contents.shouldContain("import \"acme/user/v1/GetUserResponse.proto\";")
                contents.shouldContain(
                    "rpc GetUser (acme.user.v1.GetUserRequest) returns (acme.user.v1.GetUserResponse);",
                )
            }
        }
    })
