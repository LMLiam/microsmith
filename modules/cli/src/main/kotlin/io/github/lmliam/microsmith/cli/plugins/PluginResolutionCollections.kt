package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal fun List<Path>.normalizePluginClasspath(): List<Path> =
    map { path -> path.toAbsolutePath().normalize() }.distinct()

internal fun Map<String, String>.toRemoteArtifactLockEntries(): List<LockEntry> = entries.map { (key, checksum) ->
    LockEntry(kind = REMOTE_ARTIFACT_KIND, key = key, checksum = checksum)
}

internal fun List<LockEntry>.toLockKeys(): Set<LockKey> =
    map { entry -> LockKey(kind = entry.kind, key = entry.key) }.toSet()
