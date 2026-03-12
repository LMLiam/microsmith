package me.liam.microsmith.cli.plugins

import java.nio.file.Path

internal data class RemotePluginResolution(
    val classpath: List<Path>,
    val rootLockEntries: List<LockEntry>,
    val remoteArtifactChecksums: Map<String, String>,
)
