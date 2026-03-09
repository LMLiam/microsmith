package me.liam.microsmith.build.quality

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.nio.file.Path

class RepositoryQualityPluginFunctionalTests : StringSpec() {
    init {
        "apply registers verify task and wires root check" {
            val projectDir = Files.createTempDirectory("repository-quality-plugin-root")

            projectDir.writeFile("settings.gradle.kts", "rootProject.name = \"sample-root\"")
            projectDir.writeFile(
                "build.gradle.kts",
                """
                plugins {
                    id("me.liam.microsmith.repository-quality")
                }
                """.trimIndent(),
            )

            val taskHelp = gradleRunner(projectDir)
                .withArguments("help", "--task", "verifyRepositoryStandards")
                .build()
            val dryRun = gradleRunner(projectDir)
                .withArguments("check", "--dry-run")
                .build()

            taskHelp.output shouldContain "verifyRepositoryStandards"
            taskHelp.output shouldContain "Verifies repository structural Kotlin quality guardrails."
            dryRun.output shouldContain "verifyRepositoryStandards"
        }

        "apply rejects non-root projects" {
            val projectDir = Files.createTempDirectory("repository-quality-plugin-child")

            projectDir.writeFile(
                "settings.gradle.kts",
                """
                rootProject.name = "sample-root"
                include("child")
                """.trimIndent(),
            )
            projectDir.writeFile("build.gradle.kts", "")
            projectDir.writeFile(
                "child/build.gradle.kts",
                """
                plugins {
                    id("me.liam.microsmith.repository-quality")
                }
                """.trimIndent(),
            )

            val failure = gradleRunner(projectDir)
                .withArguments("help")
                .buildAndFail()

            failure.output shouldContain "must be applied to the root project"
        }
    }

    private fun gradleRunner(projectDir: Path): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
}

private fun Path.writeFile(relativePath: String, contents: String) {
    val file = resolve(relativePath)
    Files.createDirectories(file.parent)
    Files.writeString(file, "$contents\n")
}
