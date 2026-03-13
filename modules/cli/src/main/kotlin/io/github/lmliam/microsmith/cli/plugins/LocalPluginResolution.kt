package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal data class LocalPluginResolution(val classpath: List<Path>, val lockEntries: List<LockEntry>)
