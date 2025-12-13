package me.liam.microsmith.scripting

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.scripting.MicrosmithScriptException
import java.util.Comparator
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

class MicrosmithScriptHostTests :
    StringSpec({
        "evaluates a minimal script" {
            val scriptFile = Files.createTempFile("microsmith-minimal", ".microsmith.kts")
            val cacheDir = Files.createTempDirectory("microsmith-cache")
            try {
                scriptFile.writeText(
                    """
                    val model = microsmith { }
                    model
                    """.trimIndent()
                )

                val model =
                    MicrosmithScriptHost.evaluate(
                        scriptFile,
                        MicrosmithScriptHost.currentClasspath(),
                        ScriptOptions(cacheDir = cacheDir)
                    )

                (model.keys().isEmpty()).shouldBeTrue()
            } finally {
                Files.walk(cacheDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
                scriptFile.deleteIfExists()
            }
        }

        "protobuf DSL compiles via scripting" {
            val scriptFile = Files.createTempFile("microsmith-protobuf", ".microsmith.kts")
            try {
                scriptFile.writeText(
                    """
                    microsmith {
                        schemas {
                            protobuf {
                                message("User") { int32("id") { index(1) } }
                            }
                        }
                    }
                    """.trimIndent()
                )

                val model = MicrosmithScriptHost.evaluate(scriptFile, MicrosmithScriptHost.currentClasspath())
                val schemas = model.get(SchemasExtension::class)
                (schemas?.schemas?.any { it.name.endsWith("User") } ?: false).shouldBeTrue()
            } finally {
                scriptFile.deleteIfExists()
            }
        }

        "compilation errors report line numbers" {
            val scriptFile = Files.createTempFile("microsmith-error", ".microsmith.kts")
            try {
                scriptFile.writeText(
                    """
                    microsmith {
                        schemas {
                            protobuf {
                                message("Broken") {
                                    string("name") { idx(1) }
                                }
                            }
                        }
                    }
                    """.trimIndent()
                )

                val error =
                    shouldThrow<MicrosmithScriptException> {
                        MicrosmithScriptHost.evaluate(scriptFile, MicrosmithScriptHost.currentClasspath())
                    }

                error.message shouldContain "line 5"
                error.message shouldContain "string(\"name\")"
            } finally {
                scriptFile.deleteIfExists()
            }
        }
    })
