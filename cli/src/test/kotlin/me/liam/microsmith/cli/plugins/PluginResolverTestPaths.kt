package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path

internal fun createWorkingDirectoryTempDirectory(prefix: String): Path {
    val root = workingDirectory().resolve("build/tmp/plugin-resolver-tests")
    Files.createDirectories(root)
    return Files.createTempDirectory(root, prefix)
}

internal fun relativizeFromWorkingDirectory(path: Path): Path =
    workingDirectory().relativize(path.toAbsolutePath().normalize())

private fun workingDirectory(): Path = Path.of("").toAbsolutePath().normalize()
