package io.github.lmliam.microsmith.cli.doctor

import java.nio.file.Path

internal fun defaultScriptCacheDirectory(): Path {
    val envPath = System.getenv("MICROSMITH_SCRIPT_CACHE_DIR")?.trim()?.takeIf { it.isNotEmpty() }
    if (envPath == null) {
        return Path.of(System.getProperty("user.home"), ".microsmith", "cache", "scripts")
    }
    return Path.of(envPath)
}
