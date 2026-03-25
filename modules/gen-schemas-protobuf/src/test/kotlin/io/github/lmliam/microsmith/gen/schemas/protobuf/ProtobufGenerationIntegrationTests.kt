package io.github.lmliam.microsmith.gen.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.microsmith
import io.github.lmliam.microsmith.dsl.schemas.core.schemas
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.gen.files.TemporaryDirectory
import io.github.lmliam.microsmith.gen.helpers.generateTo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files
import kotlin.io.use

class ProtobufGenerationIntegrationTests :
    StringSpec({
        "generateTo emits protobuf messages through the resolved artifact pipeline" {
            val model =
                microsmith {
                    schemas {
                        protobuf {
                            "acme.user.v1" {
                                message("User") {
                                    string("id")
                                }
                            }
                        }
                    }
                }

            TemporaryDirectory.create(prefix = "protobuf-generation-").use { outputSpace ->
                model.generateTo(outputSpace.root)
                val generatedFile = outputSpace.root.resolve("proto/acme/user/v1/User.proto")
                val contents = Files.readString(generatedFile)

                Files.exists(generatedFile) shouldBe true
                contents.shouldContain("package acme.user.v1;")
                contents.shouldContain("message User {")
                contents.shouldContain("string id = 1;")
            }
        }
    })
