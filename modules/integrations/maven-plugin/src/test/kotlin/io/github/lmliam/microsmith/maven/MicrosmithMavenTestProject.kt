package io.github.lmliam.microsmith.maven

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText

internal class MicrosmithMavenTestProject private constructor(private val rootDirectory: Path) {
    fun writeFile(relativePath: String, contents: String) {
        val file = file(relativePath)
        checkNotNull(file.parent).createDirectories()
        file.writeText("$contents\n")
    }

    fun writeResource(relativePath: String, resource: String) {
        val file = file(relativePath)
        checkNotNull(file.parent).createDirectories()
        file.writeText(
            checkNotNull(MicrosmithMavenTestProject::class.java.getResource(resource)) {
                    "Missing test resource $resource"
                }
                .readText(),
        )
    }

    fun createMojo(): MicrosmithGenerateMojo =
        MicrosmithGenerateMojo().apply {
            projectBaseDirectory = rootDirectory.toFile()
            outputDirectory = file(".").toFile()
            cacheDirectory = file("target/tmp/microsmith/cache").toFile()
        }

    fun file(relativePath: String): Path = rootDirectory.resolve(relativePath)

    companion object {
        fun create(prefix: String): MicrosmithMavenTestProject = MicrosmithMavenTestProject(createTempDirectory(prefix))
    }
}
