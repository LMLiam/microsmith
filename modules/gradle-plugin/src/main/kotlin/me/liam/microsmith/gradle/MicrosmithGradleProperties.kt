package me.liam.microsmith.gradle

import java.nio.file.Path
import java.util.Properties

internal fun Properties.readPairs(countKey: String, keyPrefix: String, valuePrefix: String): Map<String, String> =
    buildMap {
        repeat(requiredInt(countKey)) { index ->
            put(
                getProperty("$keyPrefix$index") ?: missingProperty("$keyPrefix$index"),
                getProperty("$valuePrefix$index") ?: missingProperty("$valuePrefix$index"),
            )
        }
    }

internal fun Properties.readValues(countKey: String, valuePrefix: String): List<String> =
    List(requiredInt(countKey)) { index ->
        getProperty("$valuePrefix$index") ?: missingProperty("$valuePrefix$index")
    }

internal fun Properties.requiredBoolean(key: String): Boolean {
    val value = getProperty(key) ?: missingProperty(key)
    return value.toBooleanStrictOrNull() ?: error("Property '$key' must be a boolean.")
}

internal fun Properties.requiredInt(key: String): Int {
    val value = getProperty(key) ?: missingProperty(key)
    return value.toIntOrNull() ?: error("Property '$key' must be an integer.")
}

internal fun Properties.requiredLong(key: String): Long {
    val value = getProperty(key) ?: missingProperty(key)
    return value.toLongOrNull() ?: error("Property '$key' must be a long.")
}

internal fun Properties.requiredPath(key: String): Path = Path.of(getProperty(key) ?: missingProperty(key))

internal fun missingProperty(key: String): Nothing = error("Missing required property '$key'.")
