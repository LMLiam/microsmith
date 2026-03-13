package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal data class LocalPluginJar(val artifactPath: Path, val lockKey: String)
