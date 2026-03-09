package me.liam.microsmith.build.quality

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class RepositoryQualityValidatorTests : StringSpec() {
    private val defaultValidator = RepositoryQualityValidator(RepositoryQualityPolicy.default())

    init {
        "validate counts abstract and open top level declarations" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/MixedDeclarations.kt",
                contents = """
                package example

                abstract class Alpha
                open class Bravo
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "multiple-production-types",
                    path = "module/src/main/kotlin/example/MixedDeclarations.kt",
                    message = multipleProductionTypesMessage(2),
                ),
            )
        }

        "validate counts expect actual and typealias modifiers" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/PlatformTypes.kt",
                contents = """
                package example

                expect class Alpha
                actual typealias Bravo = String
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "multiple-production-types",
                    path = "module/src/main/kotlin/example/PlatformTypes.kt",
                    message = multipleProductionTypesMessage(2),
                ),
            )
        }

        "validate ignores private and nested declarations" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/Visible.kt",
                contents = """
                package example

                private class Hidden

                class Visible {
                    class Nested
                    interface NestedContract
                }
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations.shouldBeEmpty()
        }

        "validate counts top level objects and fun interfaces" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/ApiShapes.kt",
                contents = """
                package example

                object Registry
                fun interface Action
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "multiple-production-types",
                    path = "module/src/main/kotlin/example/ApiShapes.kt",
                    message = multipleProductionTypesMessage(2),
                ),
            )
        }

        "validate reports production file line limits with override rationale" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/TooLong.kt",
                contents = """
                package example

                class TooLong
                """.trimIndent(),
            )
            val validator = RepositoryQualityValidator(
                RepositoryQualityPolicy(
                    defaultMaxProductionFileLines = 10,
                    productionFileLineOverrides = mapOf(
                        "module/src/main/kotlin/example/TooLong.kt" to ProductionFileLineOverride(
                            maxLines = 2,
                            rationale = "A narrow exception should be called out explicitly.",
                        ),
                    ),
                    multiDeclarationExemptions = emptyMap(),
                    forbiddenPackageSegments = setOf("util", "utils", "misc"),
                ),
            )

            val violations = validator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "production-file-lines",
                    path = "module/src/main/kotlin/example/TooLong.kt",
                    message = productionFileLinesMessage(
                        lineCount = 3,
                        maxLines = 2,
                        rationale = "A narrow exception should be called out explicitly.",
                    ),
                ),
            )
        }

        "validate reports forbidden package segments" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/util/Helpers.kt",
                contents = """
                package example.util

                class Helpers
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "forbidden-package-segment",
                    path = "module/src/main/kotlin/example/util/Helpers.kt",
                    message = forbiddenPackageSegmentMessage(
                        packageName = "example.util",
                        forbiddenSegment = "util",
                    ),
                ),
            )
        }

        "validate reports missing package declarations" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/NoPackage.kt",
                contents = """
                class NoPackage
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "missing-package-declaration",
                    path = "module/src/main/kotlin/example/NoPackage.kt",
                    message = missingPackageDeclarationMessage,
                ),
            )
        }

        "validate reports package path mismatches" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/right/Alpha.kt",
                contents = """
                package example.wrong

                class Alpha
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "package-path-mismatch",
                    path = "module/src/main/kotlin/example/right/Alpha.kt",
                    message = packagePathMismatchMessage(
                        packageName = "example.wrong",
                        expectedDirectory = "example/wrong",
                        actualDirectory = "example/right",
                    ),
                ),
            )
        }

        "validate reports single top level type file name mismatches" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/TypeAliasFile.kt",
                contents = """
                package example

                typealias ActualName = String
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(source.repositoryRoot, listOf(source.file))

            violations shouldContainExactly listOf(
                RepositoryQualityViolation(
                    rule = "primary-type-file-name",
                    path = "module/src/main/kotlin/example/TypeAliasFile.kt",
                    message = primaryTypeFileNameMessage(
                        fileNameWithoutExtension = "TypeAliasFile",
                        declarationName = "ActualName",
                    ),
                ),
            )
        }

        "validate sorts violations by rule and path" {
            val firstSource = productionSource(
                path = "module/src/main/kotlin/example/util/Helpers.kt",
                contents = """
                package example.util

                class Helpers
                """.trimIndent(),
            )
            val secondSource = productionSource(
                repositoryRoot = firstSource.repositoryRoot,
                path = "module/src/main/kotlin/example/MixedDeclarations.kt",
                contents = """
                package example

                class Alpha
                class Bravo
                """.trimIndent(),
            )

            val violations = defaultValidator.validate(
                firstSource.repositoryRoot,
                listOf(secondSource.file, firstSource.file),
            )

            violations.map(RepositoryQualityViolation::rule) shouldBe listOf(
                "forbidden-package-segment",
                "multiple-production-types",
            )
        }
    }

    private fun productionSource(
        path: String,
        contents: String,
        repositoryRoot: java.nio.file.Path = Files.createTempDirectory("repository-quality-validator"),
    ): TestProductionSource {
        val file = repositoryRoot.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, "$contents\n")
        return TestProductionSource(repositoryRoot, file)
    }

    private data class TestProductionSource(
        val repositoryRoot: java.nio.file.Path,
        val file: java.nio.file.Path,
    )

    private companion object {
        private fun multipleProductionTypesMessage(declarationCount: Int): String =
            "$declarationCount non-private top-level production declarations found. " +
                "Split extra production types into their own files or make tightly coupled helpers private."

        private fun productionFileLinesMessage(lineCount: Int, maxLines: Int, rationale: String): String =
            "$lineCount lines exceeds the allowed maximum of $maxLines. " +
                "Split parsing, validation, rendering, diagnostics, policy, or I/O responsibilities " +
                "before extending the file further. Override rationale: $rationale"

        private fun forbiddenPackageSegmentMessage(packageName: String, forbiddenSegment: String): String =
            "Package '$packageName' contains forbidden segment '$forbiddenSegment'. " +
                "Use a domain-led package name instead of util/utils/misc."

        private const val missingPackageDeclarationMessage =
            "Production Kotlin files must declare an explicit package. " +
                "Use a domain-led package that matches the src/main/kotlin directory."

        private fun packagePathMismatchMessage(
            packageName: String,
            expectedDirectory: String,
            actualDirectory: String,
        ): String = "Package '$packageName' maps to '$expectedDirectory', but the source file lives under " +
            "'$actualDirectory'. Keep package declarations aligned with src/main/kotlin paths."

        private fun primaryTypeFileNameMessage(fileNameWithoutExtension: String, declarationName: String): String =
            "File '$fileNameWithoutExtension.kt' does not match the single top-level production declaration " +
                "'$declarationName'. Rename the file or the declaration so the owning type remains obvious."
    }
}
