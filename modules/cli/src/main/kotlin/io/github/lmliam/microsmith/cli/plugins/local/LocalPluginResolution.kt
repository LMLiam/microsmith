package io.github.lmliam.microsmith.cli.plugins.local
import io.github.lmliam.microsmith.cli.plugins.lockfile.LockEntry
import java.nio.file.Path

internal data class LocalPluginResolution(val classpath: List<Path>, val lockEntries: List<LockEntry>)
