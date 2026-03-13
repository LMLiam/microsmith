package me.liam.microsmith.gradle

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

class MicrosmithGradlePluginFunctionalTests : StringSpec() {
    init {
        "plugin registers task and IDE configurations for Java projects" {
            val project = MicrosmithGradleFunctionalTestProject.create(
                name = "microsmith-gradle-plugin-java-wiring",
                buildScript = """
                plugins {
                    java
                    id("me.liam.microsmith.gradle")
                }

                tasks.register("verifyMicrosmithWiring") {
                    doLast {
                        val generateTask = tasks.named("microsmithGenerate").get()
                        check(generateTask is me.liam.microsmith.gradle.MicrosmithGenerateTask)

                        val ide = configurations.getByName("microsmithIde")
                        val plugins = configurations.getByName("microsmithPlugins")
                        val compileOnly = configurations.getByName("compileOnly")

                        check(ide.extendsFrom.contains(plugins))
                        check(compileOnly.extendsFrom.contains(ide))
                        check(!ide.isCanBeResolved)
                    }
                }
                """.trimIndent(),
            )

            val result = project.build("verifyMicrosmithWiring")

            result.task(":verifyMicrosmithWiring")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        "microsmithGenerate uses the default script and output layout" {
            val project = MicrosmithGradleFunctionalTestProject.create(
                name = "microsmith-gradle-plugin-default-generate",
                buildScript = """
                plugins {
                    id("me.liam.microsmith.gradle")
                }
                """.trimIndent(),
            )
            project.writeFile(
                "build.microsmith.kts",
                """
                microsmith {
                    schemas {
                        protobuf {
                            message("GradleDefaultUserCreated") {
                                int32("id") { index(1) }
                            }
                        }
                    }
                }
                """.trimIndent(),
            )

            val result = project.build("microsmithGenerate")

            result.task(":microsmithGenerate")?.outcome shouldBe TaskOutcome.SUCCESS
            project.file("build/generated/microsmith/proto/GradleDefaultUserCreated.proto").toFile().shouldExist()
            result.output shouldContain "Generated Microsmith outputs into"
        }

        "microsmithGenerate honors custom script, output, vars, and flags" {
            val project = MicrosmithGradleFunctionalTestProject.create(
                name = "microsmith-gradle-plugin-custom-generate",
                buildScript = """
                plugins {
                    id("me.liam.microsmith.gradle")
                }

                microsmithGradle {
                    scriptFile.set(layout.projectDirectory.file("schema.microsmith.kts"))
                    outputDirectory.set(layout.projectDirectory.dir("custom-generated"))
                    variables.put("entityName", "GradleConfiguredUserCreated")
                    flags.add("emit")
                }
                """.trimIndent(),
            )
            project.writeFile(
                "schema.microsmith.kts",
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

            val result = project.build("microsmithGenerate")

            result.task(":microsmithGenerate")?.outcome shouldBe TaskOutcome.SUCCESS
            project.file("custom-generated/proto/GradleConfiguredUserCreated.proto").toFile().shouldExist()
        }

        "microsmithGenerate fails with script diagnostics" {
            val project = MicrosmithGradleFunctionalTestProject.create(
                name = "microsmith-gradle-plugin-failure",
                buildScript = """
                plugins {
                    id("me.liam.microsmith.gradle")
                }
                """.trimIndent(),
            )
            project.writeFile(
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

            val result = project.buildAndFail("microsmithGenerate")

            result.output shouldContain "Microsmith generation failed"
            result.output shouldContain "Unresolved reference 'unknownCall'"
        }
    }
}
