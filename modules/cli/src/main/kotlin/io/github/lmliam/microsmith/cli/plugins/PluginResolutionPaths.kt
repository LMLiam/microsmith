package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal fun defaultPluginCacheDirectory(): Path {
    val envPath =
        System.getenv("MICROSMITH_PLUGIN_CACHE_DIR")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    if (envPath != null) {
        return Path.of(envPath)
    }

    return Path.of(System.getProperty("user.home"), ".microsmith", "cache", "plugins")
}

internal fun defaultLockfilePath(scriptPath: Path): Path {
    val normalizedScriptPath = scriptPath.toAbsolutePath().normalize()
    val lockfileBaseName = normalizedScriptPath.fileName.toString().removeSuffix(".kts")
    val lockfileName = "$lockfileBaseName.plugins.lock"
    val parent = normalizedScriptPath.parent ?: return Path.of(lockfileName)
    return parent.resolve(lockfileName)
}
