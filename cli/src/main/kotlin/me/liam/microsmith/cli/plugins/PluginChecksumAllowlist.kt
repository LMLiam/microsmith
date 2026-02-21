package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path

private const val CHECKSUM_ALLOWLIST_PATH_ENV = "MICROSMITH_PLUGIN_ALLOWLIST_FILE"
private const val ALLOWLIST_ENTRY_PARTS = 3

internal data class PluginChecksumAllowlist(val entries: Map<LockKey, String>) {
    fun assertCovers(requestedKeys: Set<LockKey>) {
        val missing = requestedKeys - entries.keys
        require(missing.isEmpty()) {
            val details =
                missing
                    .sortedWith(compareBy(LockKey::kind, LockKey::key))
                    .joinToString { "${it.kind}:${it.key}" }
            "Plugin allowlist is missing required entries: $details."
        }
    }

    fun verifyChecksum(kind: String, key: String, actualChecksum: String) {
        val lockKey = LockKey(kind = kind, key = key)
        val expectedChecksum = entries[lockKey] ?: return
        require(expectedChecksum == actualChecksum) {
            "Allowlist checksum mismatch for $kind plugin '$key'. " +
                "Expected '$expectedChecksum' but found '$actualChecksum'."
        }
    }
}

internal fun loadPluginChecksumAllowlistFromEnvironment(
    allowlistPath: String? = System.getenv(CHECKSUM_ALLOWLIST_PATH_ENV),
): PluginChecksumAllowlist? {
    val resolvedPath =
        allowlistPath
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let(Path::of)
            ?: return null
    return loadPluginChecksumAllowlistFromPath(resolvedPath)
}

internal fun loadPluginChecksumAllowlistFromPath(path: Path): PluginChecksumAllowlist {
    require(Files.exists(path) && Files.isRegularFile(path)) {
        "Plugin allowlist file '$path' does not exist or is not a file."
    }

    val entries =
        Files.readAllLines(path)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot { it.startsWith("#") }
            .map(::parseAllowlistEntry)
            .associate { entry -> LockKey(kind = entry.kind, key = entry.key) to entry.checksum }

    return PluginChecksumAllowlist(entries = entries)
}

private fun parseAllowlistEntry(line: String): LockEntry {
    val parts = line.split('|')
    require(parts.size == ALLOWLIST_ENTRY_PARTS) {
        "Invalid plugin allowlist entry '$line'. Expected <kind>|<key>|<sha256>."
    }

    val kind = parts[0]
    val key = parts[1]
    val checksum = parts[2]
    require(kind == REMOTE_KIND || kind == LOCAL_KIND) {
        "Invalid plugin allowlist entry kind '$kind'."
    }
    require(key.isNotBlank()) { "Plugin allowlist entry key must not be blank." }
    require(isSha256(checksum)) {
        "Plugin allowlist checksum for '$key' is invalid."
    }

    return LockEntry(kind = kind, key = key, checksum = checksum)
}
