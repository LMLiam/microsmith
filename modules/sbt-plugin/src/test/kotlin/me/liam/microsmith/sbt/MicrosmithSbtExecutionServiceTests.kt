package me.liam.microsmith.sbt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure

class MicrosmithSbtExecutionServiceTests : StringSpec() {
    init {
        "execute generates outputs from the configured script" {
            val fixture = MicrosmithSbtTestProject.create("microsmith-sbt-plugin-generate")
            fixture.writeFile(
                "build.microsmith.kts",
                """
                microsmith {
                    schemas {
                        protobuf {
                            message("SbtUserCreated") {
                                int32("id") { index(1) }
                            }
                        }
                    }
                }
                """.trimIndent(),
            )

            val result = MicrosmithSbtExecutionService().execute(fixture.executionConfiguration())

            result.outputDirectory shouldBe fixture.file("target/generated/microsmith")
            fixture.file("target/generated/microsmith/proto/SbtUserCreated.proto").toFile().shouldExist()
        }

        "execute forwards vars and flags to the script" {
            val fixture = MicrosmithSbtTestProject.create("microsmith-sbt-plugin-vars-flags")
            fixture.writeFile(
                "build.microsmith.kts",
                """
                val entityName = requireVar("entityName")
                if (!hasFlag("emit")) {
                    error("Expected emit flag")
                }

                emit(
                    microsmith {
                        schemas {
                            protobuf {
                                message(entityName) {
                                    int32("id") { index(1) }
                                }
                            }
                        }
                    },
                )
                """.trimIndent(),
            )

            val result = MicrosmithSbtExecutionService().execute(
                fixture.executionConfiguration(
                    outputDirectory = fixture.file("target/generated/custom"),
                    variables = mapOf("entityName" to "SbtConfiguredUserCreated"),
                    flags = setOf("emit"),
                ),
            )

            result.outputDirectory shouldBe fixture.file("target/generated/custom")
            fixture.file("target/generated/custom/proto/SbtConfiguredUserCreated.proto").toFile().shouldExist()
        }

        "script compilation failures surface as MicrosmithSbtScriptFailureException" {
            val fixture = MicrosmithSbtTestProject.create("microsmith-sbt-plugin-compilation-failure")
            fixture.writeFile(
                "build.microsmith.kts",
                """
                microsmith {
                    schemas {
                        protobuf {
                            unknownCall()
                        }
                    }
                }
                """.trimIndent(),
            )

            val error = shouldThrow<MicrosmithSbtScriptFailureException> {
                MicrosmithSbtExecutionService().execute(fixture.executionConfiguration())
            }

            error.message shouldContain "Microsmith generation failed"
            error.message shouldContain "Unresolved reference 'unknownCall'"
        }

        "host failures surface as MicrosmithSbtHostFailureException" {
            val fixture = MicrosmithSbtTestProject.create("microsmith-sbt-plugin-host-failure")
            fixture.writeFile("build.microsmith.kts", "emit(microsmith { })")
            val service = MicrosmithSbtExecutionService(
                scriptHostRunner = MicrosmithSbtScriptHostRunner { _, _ ->
                    ScriptRunFailure(listOf("Host failure"), ScriptFailureType.HOST)
                },
            )

            val error = shouldThrow<MicrosmithSbtHostFailureException> {
                service.execute(fixture.executionConfiguration())
            }

            error.message shouldContain "Microsmith generation failed"
            error.message shouldContain "Host failure"
        }

        "generic runtime failures surface as MicrosmithSbtHostFailureException with the original cause" {
            val fixture = MicrosmithSbtTestProject.create("microsmith-sbt-plugin-runtime-failure")
            fixture.writeFile("build.microsmith.kts", "emit(microsmith { })")
            val failure = RuntimeException("Unexpected runtime failure")
            val service = MicrosmithSbtExecutionService(
                scriptHostRunner = MicrosmithSbtScriptHostRunner { _, _ -> throw failure },
            )

            val error = shouldThrow<MicrosmithSbtHostFailureException> {
                service.execute(fixture.executionConfiguration())
            }

            error.message shouldContain "Microsmith sbt plugin failed before generation completed."
            error.cause shouldBe failure
        }
    }
}
