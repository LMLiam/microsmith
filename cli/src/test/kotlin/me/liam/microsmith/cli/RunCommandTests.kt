package me.liam.microsmith.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import me.liam.microsmith.scripting.MicrosmithScriptHost
import me.liam.microsmith.scripting.ScriptDependencyResolver

class RunCommandTests :
    StringSpec({
        "runs script and writes output" {
            val scriptFile = Files.createTempFile("microsmith-cli-run", ".microsmith.kts")
            val outputDir = Files.createTempDirectory("microsmith-cli-out")
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

                val cli =
                    MicrosmithCli(
                        scriptHost = MicrosmithScriptHost,
                        dependencyResolver = ScriptDependencyResolver.None
                    )
                val status =
                    cli.run(
                        listOf(
                            "run",
                            scriptFile.toString(),
                            "--out",
                            outputDir.toString(),
                            "--generator",
                            "schemas"
                        )
                    )

                status shouldBe 0
                val generated = outputDir.resolve(Path.of("proto/User.proto"))
                Files.exists(generated).shouldBeTrue()
            } finally {
                scriptFile.deleteIfExists()
                Files.walk(outputDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            }
        }

        "returns generation failure exit code" {
            val scriptFile = Files.createTempFile("microsmith-cli-error", ".microsmith.kts")
            try {
                scriptFile.writeText("microsmith { }")

                val cli =
                    MicrosmithCli(
                        scriptHost = MicrosmithScriptHost,
                        dependencyResolver = ScriptDependencyResolver.None
                    )

                val status = cli.run(listOf("run", scriptFile.toString(), "--generator", "failing"))
                status shouldBe 3
            } finally {
                scriptFile.deleteIfExists()
            }
        }
    })
