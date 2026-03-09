package me.liam.microsmith.build.quality

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.TaskOutcome

class RepositoryQualityTaskFunctionalTests : StringSpec() {
    init {
        "verifyRepositoryStandards passes for a compliant project" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-compliant")
            project.writeFile(
                "src/main/kotlin/example/Alpha.kt",
                """
                package example

                class Alpha
                """.trimIndent(),
            )

            val result = project.build("verifyRepositoryStandards")

            result.task(":verifyRepositoryStandards")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Repository structural Kotlin quality guardrails passed."
        }

        "verifyRepositoryStandards fails for multiple production declarations" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-multi-type")
            project.writeFile(
                "src/main/kotlin/example/MixedDeclarations.kt",
                """
                package example

                class Alpha
                class Bravo
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[multiple-production-types] src/main/kotlin/example/MixedDeclarations.kt"
            result.output shouldContain multipleProductionTypesRemediation
            result.output shouldContain policyExceptionRemediationPrefix
        }

        "verifyRepositoryStandards fails for production file line limit violations" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-line-limit")
            project.writeFile(
                "src/main/kotlin/example/TooLong.kt",
                buildLongSource(fileName = "TooLong"),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[production-file-lines] src/main/kotlin/example/TooLong.kt"
            result.output shouldContain lineLimitRemediation
        }

        "verifyRepositoryStandards fails for forbidden package segments" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-package-segment")
            project.writeFile(
                "src/main/kotlin/example/util/Helpers.kt",
                """
                package example.util

                class Helpers
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[forbidden-package-segment] src/main/kotlin/example/util/Helpers.kt"
            result.output shouldContain "Package 'example.util' contains forbidden segment 'util'."
        }

        "verifyRepositoryStandards ignores files under build directories" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-build-ignored")
            project.writeFile(
                "src/main/kotlin/example/Alpha.kt",
                """
                package example

                class Alpha
                """.trimIndent(),
            )
            project.writeFile(
                "build/generated/src/main/kotlin/example/Generated.kt",
                """
                package example

                class GeneratedAlpha
                class GeneratedBravo
                """.trimIndent(),
            )

            val result = project.build("verifyRepositoryStandards")

            result.task(":verifyRepositoryStandards")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Repository structural Kotlin quality guardrails passed."
        }
    }

    private fun buildLongSource(fileName: String): String = buildString {
        appendLine("package example")
        appendLine()
        appendLine("class $fileName {")
        repeat(171) { index ->
            appendLine("    fun line$index(): Int = $index")
        }
        appendLine("}")
    }

    private companion object {
        private const val multipleProductionTypesRemediation =
            "Split extra production types into their own files or make tightly coupled helpers private."
        private const val lineLimitRemediation =
            "Split parsing, validation, rendering, diagnostics, policy, or I/O responsibilities " +
                "before extending the file further."
        private const val policyExceptionRemediationPrefix =
            "Fix the structural issue, or if the exception is truly justified, " +
                "update RepositoryQualityPolicy"
    }
}
