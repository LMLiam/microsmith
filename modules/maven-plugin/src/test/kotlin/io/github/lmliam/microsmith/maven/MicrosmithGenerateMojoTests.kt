package io.github.lmliam.microsmith.maven

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import java.util.Properties

class MicrosmithGenerateMojoTests : StringSpec() {
    init {
        "execute generates outputs from the configured script" {
            val fixture = MicrosmithMavenTestProject.create("maven-plugin-generate")
            fixture.writeFile(
                "build.microsmith.kts",
                """
                microsmith {
                    schemas {
                        protobuf {
                            message("MavenUserCreated") {
                                int32("id") { index(1) }
                            }
                        }
                    }
                }
                """.trimIndent(),
            )

            val mojo = fixture.createMojo()

            mojo.execute()

            fixture.file("target/generated/microsmith/proto/MavenUserCreated.proto").toFile().shouldExist()
        }

        "execute forwards vars and flags to the script" {
            val fixture = MicrosmithMavenTestProject.create("maven-plugin-vars-flags")
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
            val properties = Properties().apply {
                setProperty("entityName", "MavenConfiguredUserCreated")
            }

            val mojo = fixture.createMojo().apply {
                outputDirectory = fixture.file("custom-generated").toFile()
                variables = properties
                flags = listOf(" emit ", "")
            }

            mojo.execute()

            fixture.file("custom-generated/proto/MavenConfiguredUserCreated.proto").toFile().shouldExist()
        }

        "script compilation failures surface as MojoFailureException" {
            val fixture = MicrosmithMavenTestProject.create("maven-plugin-compilation-failure")
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

            val mojo = fixture.createMojo()

            val error = shouldThrow<MojoFailureException> {
                mojo.execute()
            }

            error.message shouldContain "Microsmith generation failed"
            error.message shouldContain "Unresolved reference 'unknownCall'"
        }

        "host failures surface as MojoExecutionException" {
            val fixture = MicrosmithMavenTestProject.create("maven-plugin-host-failure")
            fixture.writeFile("build.microsmith.kts", "emit(microsmith { })")
            val mojo = fixture.createMojo().apply {
                scriptHostRunner = MicrosmithScriptHostRunner { _, _ ->
                    ScriptRunFailure(listOf("Host failure"), ScriptFailureType.HOST)
                }
            }

            val error = shouldThrow<MojoExecutionException> {
                mojo.execute()
            }

            error.message shouldContain "Microsmith generation failed"
            error.message shouldContain "Host failure"
        }

        "unexpected execution failures surface as MojoExecutionException with the original cause" {
            val fixture = MicrosmithMavenTestProject.create("maven-plugin-unexpected-failure")
            fixture.writeFile("build.microsmith.kts", "emit(microsmith { })")
            val failure = IllegalStateException("Unexpected host failure")
            val mojo = fixture.createMojo().apply {
                scriptHostRunner = MicrosmithScriptHostRunner { _, _ -> throw failure }
            }

            val error = shouldThrow<MojoExecutionException> {
                mojo.execute()
            }

            error.message shouldContain "Microsmith Maven plugin failed before generation completed."
            error.cause shouldBe failure
        }

        "generic runtime failures surface as MojoExecutionException with the original cause" {
            val fixture = MicrosmithMavenTestProject.create("maven-plugin-runtime-failure")
            fixture.writeFile("build.microsmith.kts", "emit(microsmith { })")
            val failure = RuntimeException("Unexpected runtime failure")
            val mojo = fixture.createMojo().apply {
                scriptHostRunner = MicrosmithScriptHostRunner { _, _ -> throw failure }
            }

            val error = shouldThrow<MojoExecutionException> {
                mojo.execute()
            }

            error.message shouldContain "Microsmith Maven plugin failed before generation completed."
            error.cause shouldBe failure
        }
    }
}
