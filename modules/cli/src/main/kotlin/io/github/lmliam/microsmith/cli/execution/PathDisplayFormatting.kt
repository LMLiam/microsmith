package io.github.lmliam.microsmith.cli.execution

import java.nio.file.Path

internal fun List<Path>.formatForDisplay(projectRoot: Path): String = joinToString(separator = ", ") { path ->
    projectRoot.relativize(path.toAbsolutePath().normalize()).toString()
}
