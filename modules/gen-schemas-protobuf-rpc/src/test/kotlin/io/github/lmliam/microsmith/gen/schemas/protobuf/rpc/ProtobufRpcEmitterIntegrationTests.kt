package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasBuilder
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.github.lmliam.microsmith.gen.files.TemporaryDirectory
import io.github.lmliam.microsmith.gen.schemas.protobuf.emission.ProtobufEmitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.readText
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
                val contents = space.root.resolve(generated.relativePath).readText()

                contents.shouldContain("import \"GetUserRequest.proto\";")
                contents.shouldContain("import \"GetUserResponse.proto\";")
                contents.shouldContain("service UserService {")
                contents.shouldContain("rpc GetUser (GetUserRequest) returns (GetUserResponse);")
            }
        }
    })
