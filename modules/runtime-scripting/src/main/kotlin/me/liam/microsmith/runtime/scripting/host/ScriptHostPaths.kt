package me.liam.microsmith.runtime.scripting.host

import java.nio.file.Path

internal object ScriptHostPaths {
    fun defaultCacheDirectory(): Path {
        val envPath =
            System.getenv("MICROSMITH_SCRIPT_CACHE_DIR")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        if (envPath != null) {
            return Path.of(envPath)
        }

        return Path.of(System.getProperty("user.home"), ".microsmith", "cache", "scripts")
    }
}
