package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal data class ResolvedRemotePlugin(
    val rootArtifactPath: Path,
    val classpath: List<Path>,
    val artifacts: List<ResolvedRemoteArtifact>,
)
