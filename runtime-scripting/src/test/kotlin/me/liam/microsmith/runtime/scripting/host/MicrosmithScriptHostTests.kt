package me.liam.microsmith.runtime.scripting.host

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class MicrosmithScriptHostTests :
    StringSpec({
        "runs script returning MicrosmithModel and generates protobuf output" {
            val tempDir = createTempDirectory("microsmith-script-host")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")

                script.writeText(
                    """
                    microsmith {
                        schemas {
                            protobuf {
                                "pkg" {
                                    message("User") {
                                        int32("id") { index(1) }
                                    }
                                }
                            }
                        }
                    }
                    """.trimIndent()
                )

                val host = MicrosmithScriptHost(cacheDirectory = cache)

                val firstRun =
                    host.run(
                        ScriptRunRequest(
                            script = script,
                            outputDir = output,
                            variables = emptyMap(),
                            flags = emptySet()
                        )
                    )
                val firstSuccess = firstRun.shouldBeTypeOf<ScriptRunSuccess>()
                firstSuccess.cacheHit shouldBe false

                val generatedFile = output.resolve("proto/pkg/User.proto")
                generatedFile.exists() shouldBe true
                generatedFile.readText().shouldContain("message User")

                val secondRun =
                    host.run(
                        ScriptRunRequest(
                            script = script,
                            outputDir = output,
                            variables = emptyMap(),
                            flags = emptySet()
                        )
                    )
                val secondSuccess = secondRun.shouldBeTypeOf<ScriptRunSuccess>()
                secondSuccess.cacheHit shouldBe true
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "supports emit helper with vars and flags from CLI context" {
            val tempDir = createTempDirectory("microsmith-script-host-context")
            try {
                val script = tempDir.resolve("context.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")

                script.writeText(
                    """
                    val schemaName = requireVar("schema")
                    if (!hasFlag("emit")) {
                        error("emit flag is required")
                    }

                    val model =
                        microsmith {
                            schemas {
                                protobuf {
                                    message(schemaName) {
                                        int32("id") { index(1) }
                                    }
                                }
                            }
                        }
                    emit(model)
                    """.trimIndent()
                )

                val host = MicrosmithScriptHost(cacheDirectory = cache)
                val result =
                    host.run(
                        ScriptRunRequest(
                            script = script,
                            outputDir = output,
                            variables = mapOf("schema" to "AuditRecord"),
                            flags = setOf("emit")
                        )
                    )

                result.shouldBeTypeOf<ScriptRunSuccess>()
                val generatedFile = output.resolve("proto/AuditRecord.proto")
                Files.exists(generatedFile) shouldBe true
                generatedFile.readText().shouldContain("message AuditRecord")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "returns deterministic diagnostics for script errors" {
            val tempDir = createTempDirectory("microsmith-script-host-error")
            try {
                val script = tempDir.resolve("broken.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")

                script.writeText(
                    """
                    microsmith {
                        unknownDsl()
                    }
                    """.trimIndent()
                )

                val host = MicrosmithScriptHost(cacheDirectory = cache)
                val result =
                    host.run(
                        ScriptRunRequest(
                            script = script,
                            outputDir = output,
                            variables = emptyMap(),
                            flags = emptySet()
                        )
                    )

                val failure = result.shouldBeTypeOf<ScriptRunFailure>()
                failure.diagnostics.joinToString("\n").shouldContain("[error]")
                failure.diagnostics.joinToString("\n").shouldContain("broken.microsmith.kts")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })

