package me.liam.microsmith.cli.plugins

import java.nio.file.Path

internal data class ResolvedRemoteArtifact(val lockKey: String, val artifactPath: Path)
