package io.github.lmliam.microsmith.gradle

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

internal class MicrosmithGradleFunctionalTestProject private constructor(private val rootDirectory: Path) {
    fun writeFile(relativePath: String, contents: String) {
        val file = rootDirectory.resolve(relativePath)
        val parent = file.parent
        if (parent != null) {
            parent.createDirectories()
        }
        file.writeText("$contents\n")
    }

    fun writeResource(relativePath: String, resource: String) {
        val file = file(relativePath)
        checkNotNull(file.parent).createDirectories()
        file.writeText(testResourceText(resource))
    }

    fun build(vararg arguments: String): BuildResult = gradleRunner(*arguments).build()

    fun buildAndFail(vararg arguments: String): BuildResult = gradleRunner(*arguments).buildAndFail()

    fun file(relativePath: String): Path = rootDirectory.resolve(relativePath)

    private fun gradleRunner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(rootDirectory.toFile())
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")

    companion object {
        fun create(name: String, buildScriptResource: String): MicrosmithGradleFunctionalTestProject {
            val directory = createTempDirectory(name)
            val project = MicrosmithGradleFunctionalTestProject(directory)
            project.writeFile("settings.gradle.kts", "rootProject.name = \"$name\"")
            project.writeFile(
                "build.gradle.kts",
                listOf(
                        testResourceText(buildScriptResource),
                        MicrosmithGradleFunctionalTestRuntimeClasspath.buildScriptDependencyBlock(),
                    )
                    .joinToString(separator = "\n"),
            )
            return project
        }
    }
}

private fun testResourceText(resource: String): String =
    checkNotNull(MicrosmithGradleFunctionalTestProject::class.java.getResource(resource)) {
            "Missing test resource $resource"
        }
        .readText()
