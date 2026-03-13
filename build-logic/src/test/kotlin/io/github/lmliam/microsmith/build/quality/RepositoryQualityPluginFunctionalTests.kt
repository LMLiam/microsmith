package io.github.lmliam.microsmith.build.quality

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

class RepositoryQualityPluginFunctionalTests : StringSpec() {
    init {
        "apply registers verify task and wires root check" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-plugin-root")

            val taskHelp = project.build("help", "--task", "verifyRepositoryStandards")
            val dryRun = project.build("check", "--dry-run")

            taskHelp.output shouldContain "verifyRepositoryStandards"
            taskHelp.output shouldContain "Verifies repository structural Kotlin quality guardrails."
            dryRun.output shouldContain "verifyRepositoryStandards"
        }

        "apply rejects non-root projects" {
            val project = RepositoryQualityFunctionalTestProject.multiProject(
                name = "repository-quality-plugin-child",
                childName = "child",
            )
            project.writeFile(
                "child/build.gradle.kts",
                """
                plugins {
                    id("io.github.lmliam.microsmith.repository-quality")
                }
                """.trimIndent(),
            )

            val failure = project.buildAndFail("help")

            failure.output shouldContain "must be applied to the root project"
        }
    }
}
