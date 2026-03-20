package io.github.lmliam.microsmith.sbt

import java.nio.file.Files
import java.nio.file.Path

class MicrosmithSbtTestProject private constructor(
    private val rootDirectory: Path,
) {
    fun writeFile(relativePath: String, contents: String) {
        val file = file(relativePath)
        Files.createDirectories(checkNotNull(file.parent))
        Files.writeString(file, "$contents\n")
    }

    fun file(relativePath: String): Path = rootDirectory.resolve(relativePath)

    fun executionConfiguration(
        scriptFile: Path = file("build.microsmith.kts"),
        outputDirectory: Path = file("."),
        cacheDirectory: Path = file("target/tmp/microsmith/cache"),
        variables: Map<String, String> = emptyMap(),
        flags: Set<String> = emptySet(),
    ): MicrosmithSbtExecutionConfiguration = MicrosmithSbtExecutionConfiguration(
        baseDirectory = rootDirectory,
        scriptFile = scriptFile,
        outputDirectory = outputDirectory,
        cacheDirectory = cacheDirectory,
        variables = variables,
        flags = flags,
    )

    companion object {
        fun create(prefix: String): MicrosmithSbtTestProject =
            MicrosmithSbtTestProject(Files.createTempDirectory(prefix))
    }
}
