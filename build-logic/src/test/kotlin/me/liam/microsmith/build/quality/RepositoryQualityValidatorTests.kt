package me.liam.microsmith.build.quality

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryQualityValidatorTests {
    private val validator = RepositoryQualityValidator(RepositoryQualityPolicy.default())

    @Test
    fun `validate counts abstract and open top level declarations`() {
        val source = productionSource(
            path = "module/src/main/kotlin/example/MixedDeclarations.kt",
            contents = """
            package example

            abstract class Alpha
            open class Bravo
            """.trimIndent(),
        )

        val violations = validator.validate(source.repositoryRoot, listOf(source.file))

        assertEquals(
            listOf(
                RepositoryQualityViolation(
                    rule = "multiple-production-types",
                    path = "module/src/main/kotlin/example/MixedDeclarations.kt",
                    message = "2 non-private top-level production declarations found. Split extra production types into their own files or make tightly coupled helpers private.",
                ),
            ),
            violations,
        )
    }

    @Test
    fun `validate counts expect actual and typealias modifiers`() {
        val source = productionSource(
            path = "module/src/main/kotlin/example/PlatformTypes.kt",
            contents = """
            package example

            expect class Alpha
            actual typealias Bravo = String
            """.trimIndent(),
        )

        val violations = validator.validate(source.repositoryRoot, listOf(source.file))

        assertEquals(
            listOf(
                RepositoryQualityViolation(
                    rule = "multiple-production-types",
                    path = "module/src/main/kotlin/example/PlatformTypes.kt",
                    message = "2 non-private top-level production declarations found. Split extra production types into their own files or make tightly coupled helpers private.",
                ),
            ),
            violations,
        )
    }

    private fun productionSource(path: String, contents: String): TestProductionSource {
        val repositoryRoot = Files.createTempDirectory("repository-quality-validator")
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
