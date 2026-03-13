package me.liam.microsmith.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.nio.file.Files
import java.nio.file.Path

internal class MicrosmithGradleFunctionalTestProject private constructor(
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

    fun file(relativePath: String): Path = rootDirectory.resolve(relativePath)

    private fun gradleRunner(vararg arguments: String): GradleRunner = GradleRunner.create()
        .withProjectDir(rootDirectory.toFile())
        .withPluginClasspath()
        .withArguments(*arguments, "--stacktrace")

    companion object {
        fun create(name: String, buildScript: String): MicrosmithGradleFunctionalTestProject {
            val directory = Files.createTempDirectory(name)
            val project = MicrosmithGradleFunctionalTestProject(directory)
            project.writeFile("settings.gradle.kts", "rootProject.name = \"$name\"")
            project.writeFile(
                "build.gradle.kts",
                listOf(
                    buildScript,
                    MicrosmithGradleFunctionalTestRuntimeClasspath.buildScriptDependencyBlock(),
                ).joinToString(separator = "\n\n"),
            )
            return project
        }
    }
}
