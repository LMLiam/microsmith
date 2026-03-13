package io.github.lmliam.microsmith.build.quality

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.nio.file.Path

internal class RepositoryQualityFunctionalTestProject private constructor(
    private val rootDirectory: Path,
) {
    fun writeFile(relativePath: String, contents: String) {
        val file = rootDirectory.resolve(relativePath)
        val parent = file.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }
        Files.writeString(file, "$contents\n")
    }

    fun build(vararg arguments: String): BuildResult = gradleRunner(*arguments).build()

    fun buildAndFail(vararg arguments: String): BuildResult = gradleRunner(*arguments).buildAndFail()

    private fun gradleRunner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(rootDirectory.toFile())
        .withPluginClasspath()
        .withArguments(*arguments)

    companion object {
        fun rootProject(name: String): RepositoryQualityFunctionalTestProject {
            val directory = Files.createTempDirectory(name)
            val project = RepositoryQualityFunctionalTestProject(directory)
            project.writeFile("settings.gradle.kts", "rootProject.name = \"$name\"")
            project.writeFile(
                "build.gradle.kts",
                """
                plugins {
                    id("io.github.lmliam.microsmith.repository-quality")
                }
                """.trimIndent(),
            )
            return project
        }

        fun multiProject(name: String, childName: String): RepositoryQualityFunctionalTestProject {
            val directory = Files.createTempDirectory(name)
            val project = RepositoryQualityFunctionalTestProject(directory)
            project.writeFile(
                "settings.gradle.kts",
                """
                rootProject.name = "$name"
                include("$childName")
                """.trimIndent(),
            )
            project.writeFile("build.gradle.kts", "")
            return project
        }
    }
}
