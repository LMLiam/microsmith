package io.github.lmliam.microsmith.build.quality

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
            result.output shouldContain MULTIPLE_PRODUCTION_TYPES_REMEDIATION
            result.output shouldContain POLICY_EXCEPTION_REMEDIATION_PREFIX
        }

        "verifyRepositoryStandards fails for production file line limit violations" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-line-limit")
            project.writeFile(
                "src/main/kotlin/example/TooLong.kt",
                buildLongSource(fileName = "TooLong"),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[production-file-lines] src/main/kotlin/example/TooLong.kt"
            result.output shouldContain LINE_LIMIT_REMEDIATION
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

        "verifyRepositoryStandards fails for missing package declarations" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-missing-package")
            project.writeFile(
                "src/main/kotlin/example/NoPackage.kt",
                """
                class NoPackage
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[missing-package-declaration] src/main/kotlin/example/NoPackage.kt"
            result.output shouldContain MISSING_PACKAGE_DECLARATION_REMEDIATION
        }

        "verifyRepositoryStandards accepts package declarations with trailing comments" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-package-comment")
            project.writeFile(
                "src/main/kotlin/example/Alpha.kt",
                """
                package example // comment

                class Alpha
                """.trimIndent(),
            )

            val result = project.build("verifyRepositoryStandards")

            result.task(":verifyRepositoryStandards")?.outcome shouldBe TaskOutcome.SUCCESS
            result.output shouldContain "Repository structural Kotlin quality guardrails passed."
        }

        "verifyRepositoryStandards fails for package path mismatches" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-package-path")
            project.writeFile(
                "src/main/kotlin/example/right/Alpha.kt",
                """
                package example.wrong

                class Alpha
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[package-path-mismatch] src/main/kotlin/example/right/Alpha.kt"
            result.output shouldContain "Package 'example.wrong' maps to 'example/wrong'"
        }

        "verifyRepositoryStandards fails when single top level type does not match file name" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-file-name")
            project.writeFile(
                "src/main/kotlin/example/TypeAliasFile.kt",
                """
                package example

                typealias ActualName = String
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[primary-type-file-name] src/main/kotlin/example/TypeAliasFile.kt"
            result.output shouldContain PRIMARY_TYPE_FILE_NAME_PREFIX
        }

        "verifyRepositoryStandards fails for file name mismatches on inline annotated declarations" {
            val project = RepositoryQualityFunctionalTestProject.rootProject(
                "repository-quality-inline-annotation-file-name",
            )
            project.writeFile(
                "src/main/kotlin/example/WrongName.kt",
                """
                package example

                @Deprecated("use something else", ReplaceWith("replacement()")) class ActualName
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[primary-type-file-name] src/main/kotlin/example/WrongName.kt"
            result.output shouldContain "WrongName.kt"
            result.output shouldContain "ActualName"
        }

        "verifyRepositoryStandards fails for indented top level declarations" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-indented-top-level")
            project.writeFile(
                "src/main/kotlin/example/IndentedDeclarations.kt",
                """
                package example

                  class Alpha
                  class Bravo
                """.trimIndent(),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[multiple-production-types] src/main/kotlin/example/IndentedDeclarations.kt"
            result.output shouldContain MULTIPLE_PRODUCTION_TYPES_REMEDIATION
        }

        "verifyRepositoryStandards scans build-logic production sources" {
            val project = RepositoryQualityFunctionalTestProject.rootProject("repository-quality-build-logic-scan")
            project.writeFile(
                "build-logic/src/main/kotlin/example/TooLong.kt",
                buildLongSource(fileName = "TooLong"),
            )

            val result = project.buildAndFail("verifyRepositoryStandards")

            result.output shouldContain "[production-file-lines] build-logic/src/main/kotlin/example/TooLong.kt"
            result.output shouldContain LINE_LIMIT_REMEDIATION
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
        private const val MULTIPLE_PRODUCTION_TYPES_REMEDIATION =
            "Split extra production types into their own files or make tightly coupled helpers private."
        private const val LINE_LIMIT_REMEDIATION =
            "Split parsing, validation, rendering, diagnostics, policy, or I/O responsibilities " +
                "before extending the file further."
        private const val MISSING_PACKAGE_DECLARATION_REMEDIATION =
            "Production Kotlin files must declare an explicit package."
        private const val POLICY_EXCEPTION_REMEDIATION_PREFIX =
            "Fix the structural issue, or if the exception is truly justified, " +
                "update RepositoryQualityPolicy"
        private const val PRIMARY_TYPE_FILE_NAME_PREFIX =
            "File 'TypeAliasFile.kt' does not match the single top-level production declaration"
    }
}
