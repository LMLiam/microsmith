package me.liam.microsmith.maven

import java.nio.file.Files
import java.nio.file.Path

internal class MicrosmithMavenTestProject private constructor(
    private val rootDirectory: Path,
) {
    fun writeFile(relativePath: String, contents: String) {
        val file = file(relativePath)
        Files.createDirectories(checkNotNull(file.parent))
        Files.writeString(file, "$contents\n")
    }

    fun createMojo(): MicrosmithGenerateMojo = MicrosmithGenerateMojo().apply {
        projectBaseDirectory = rootDirectory.toFile()
        outputDirectory = file("target/generated/microsmith").toFile()
        cacheDirectory = file("target/tmp/microsmith/cache").toFile()
    }

    fun file(relativePath: String): Path = rootDirectory.resolve(relativePath)

    companion object {
        fun create(prefix: String): MicrosmithMavenTestProject =
            MicrosmithMavenTestProject(Files.createTempDirectory(prefix))
    }
}
