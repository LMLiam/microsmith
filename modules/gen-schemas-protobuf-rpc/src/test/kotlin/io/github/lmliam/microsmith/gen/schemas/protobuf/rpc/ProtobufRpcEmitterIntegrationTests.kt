package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.github.lmliam.microsmith.gen.files.TemporaryDirectory
import io.github.lmliam.microsmith.gen.schemas.protobuf.emission.ProtobufEmitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.use

class ProtobufRpcEmitterIntegrationTests :
    StringSpec({
        val emitter = ProtobufEmitter()

        "emits service declarations with message imports" {
            val serviceSchema =
                SchemasBuilder().apply {
                    protobuf {
                        message("GetUserRequest")
                        message("GetUserResponse")
                        service("UserService") {
                            "GetUser" { "GetUserRequest" to "GetUserResponse" }
                        }
                    }
                }.toExtension().schemas.filterIsInstance<ProtobufSchema>().first { it.name == "UserService" }

            TemporaryDirectory.create().use { space ->
                val generated = with(emitter) { serviceSchema.emit(space) }
                val contents = generated.contents.toString(Charsets.UTF_8)

                generated.relativePath.toString().replace("\\", "/") shouldBe "proto/UserService.proto"
                contents.shouldContain("import \"GetUserRequest.proto\";")
                contents.shouldContain("import \"GetUserResponse.proto\";")
                contents.shouldContain("service UserService {")
                contents.shouldContain("rpc GetUser (GetUserRequest) returns (GetUserResponse);")
            }
        }

        "emits qualified service declarations under the namespaced proto path" {
            val serviceSchema =
                SchemasBuilder().apply {
                    protobuf {
                        "acme.user.v1" {
                            message("GetUserRequest")
                            message("GetUserResponse")
                            service("UserService") {
                                "GetUser" { "GetUserRequest" to "GetUserResponse" }
                            }
                        }
                    }
                }.toExtension()
                    .schemas
                    .filterIsInstance<ProtobufSchema>()
                    .first { it.name == "acme.user.v1.UserService" }

            TemporaryDirectory.create().use { space ->
                val generated = with(emitter) { serviceSchema.emit(space) }
                val contents = generated.contents.toString(Charsets.UTF_8)
                val expectedPath = "proto/acme/user/v1/UserService.proto"
                val expectedRpc =
                    "rpc GetUser (acme.user.v1.GetUserRequest) returns " +
                        "(acme.user.v1.GetUserResponse);"

                generated.relativePath.toString().replace("\\", "/") shouldBe expectedPath
                contents.shouldContain("package acme.user.v1;")
                contents.shouldContain("import \"acme/user/v1/GetUserRequest.proto\";")
                contents.shouldContain("import \"acme/user/v1/GetUserResponse.proto\";")
                contents.shouldContain(expectedRpc)
            }
        }
    })
