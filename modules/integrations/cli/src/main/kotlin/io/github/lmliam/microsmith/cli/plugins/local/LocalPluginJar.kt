package io.github.lmliam.microsmith.cli.plugins.local

import java.nio.file.Path

internal data class LocalPluginJar(val artifactPath: Path, val lockKey: String)
