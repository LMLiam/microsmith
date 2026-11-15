package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory

internal fun createWorkingDirectoryTempDirectory(prefix: String): Path {
    val root = workingDirectory().resolve("build/tmp/plugin-resolver-tests")
    root.createDirectories()
    return createTempDirectory(root, prefix)
}

internal fun relativizeFromWorkingDirectory(path: Path): Path =
    workingDirectory().relativize(path.toAbsolutePath().normalize())

private fun workingDirectory(): Path = Path.of("").toAbsolutePath().normalize()
