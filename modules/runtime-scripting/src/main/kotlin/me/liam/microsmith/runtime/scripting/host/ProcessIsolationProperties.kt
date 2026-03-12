package me.liam.microsmith.runtime.scripting.host

import java.nio.file.Path
import java.util.Properties

internal fun Properties.readIndexedList(countKey: String, keyPrefix: String): List<String> {
    val count = requiredInt(countKey)
    return (0 until count).map { index ->
        getProperty("$keyPrefix$index") ?: error("Missing indexed entry '$keyPrefix$index'.")
    }
}

internal fun Properties.readIndexedMap(countKey: String, keyPrefix: String, valuePrefix: String): Map<String, String> {
    val count = requiredInt(countKey)
    return (0 until count).associate { index ->
        val key = getProperty("$keyPrefix$index") ?: error("Missing indexed entry '$keyPrefix$index'.")
        val value = getProperty("$valuePrefix$index") ?: error("Missing indexed entry '$valuePrefix$index'.")
        key to value
    }
}

internal fun Properties.requiredBoolean(key: String): Boolean =
    requiredString(key).toBooleanStrictOrNull() ?: error("Missing or invalid '$key'.")

internal fun Properties.requiredInt(key: String): Int =
    requiredString(key).toIntOrNull() ?: error("Missing or invalid '$key'.")

internal fun Properties.requiredLong(key: String): Long =
    requiredString(key).toLongOrNull() ?: error("Missing or invalid '$key'.")

internal fun Properties.requiredPath(key: String): Path = requiredString(key).let(Path::of)

private fun Properties.requiredString(key: String): String = getProperty(key)
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?: error("Missing or invalid '$key'.")
