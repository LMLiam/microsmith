package me.liam.microsmith.cli

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Comparator
import me.liam.microsmith.scripting.MicrosmithScriptHost
import me.liam.microsmith.scripting.ScriptDependencyResolver

class RunCommandTests :
    StringSpec({
        "runs script and writes output" {
            val outputDir = Files.createTempDirectory("microsmith-cli-out")
            try {
                val scriptFile = resourcePath("basic.microsmith.kts")
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
                Files.walk(outputDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            }
        }

        "returns generation failure exit code" {
            val scriptFile = resourcePath("basic.microsmith.kts")

            val cli =
                MicrosmithCli(
                    scriptHost = MicrosmithScriptHost,
                    dependencyResolver = ScriptDependencyResolver.None
                )

            val status = cli.run(listOf("run", scriptFile.toString(), "--generator", "failing"))
            status shouldBe 3
        }
    })

private fun resourcePath(name: String): Path =
    Paths.get(
        requireNotNull(RunCommandTests::class.java.getResource("/$name")) {
            "Test resource not found: $name"
        }.toURI()
    )
