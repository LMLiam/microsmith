package io.github.lmliam.microsmith.cli.plugins.remote
import java.nio.file.Path

internal data class ResolvedRemoteArtifact(val lockKey: String, val artifactPath: Path)
