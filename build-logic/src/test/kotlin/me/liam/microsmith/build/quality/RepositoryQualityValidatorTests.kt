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
                    message = "2 non-private top-level production declarations found. Split extra production types into their own files or make tightly coupled helpers private.",
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
                    message = "2 non-private top-level production declarations found. Split extra production types into their own files or make tightly coupled helpers private.",
                ),
            )
        }

        "validate ignores private and nested declarations" {
            val source = productionSource(
                path = "module/src/main/kotlin/example/ScopedDeclarations.kt",
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
                    message = "2 non-private top-level production declarations found. Split extra production types into their own files or make tightly coupled helpers private.",
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
                    message = "3 lines exceeds the allowed maximum of 2. Split parsing, validation, rendering, diagnostics, policy, or I/O responsibilities before extending the file further. Override rationale: A narrow exception should be called out explicitly.",
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
                    message = "Package 'example.util' contains forbidden segment 'util'. Use a domain-led package name instead of util/utils/misc.",
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

            val violations = defaultValidator.validate(firstSource.repositoryRoot, listOf(secondSource.file, firstSource.file))

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
}
