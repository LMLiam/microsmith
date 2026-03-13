package io.github.lmliam.microsmith.maven

import java.nio.file.Path
import java.util.Properties

internal data class MicrosmithMavenExecutionConfiguration(
    val baseDirectory: Path,
    val scriptFile: Path,
    val outputDirectory: Path,
    val cacheDirectory: Path,
    val variables: Properties?,
    val flags: List<String>?,
)
