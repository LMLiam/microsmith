package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

internal const val LOCKFILE_VERSION = 1
internal const val MAVEN_CENTRAL_REPOSITORY = "https://repo1.maven.org/maven2"
internal const val REMOTE_KIND = "remote"
internal const val LOCAL_KIND = "local"
private const val COORDINATE_PART_COUNT = 3
private const val HEX_SHA256_LENGTH = 64

internal data class Coordinate(
    val group: String,
    val artifact: String,
    val version: String
) {
    val value: String
        get() = "$group:$artifact:$version"

    val relativeJarPath: String
        get() = "${group.replace('.', '/')}/$artifact/$version/$artifact-$version.jar"
}

internal data class LockEntry(
    val kind: String,
    val key: String,
    val checksum: String
)

internal data class LockKey(
    val kind: String,
    val key: String
)

internal data class ParsedLockfile(
    val version: Int,
    val entries: List<LockEntry>
)

internal fun parseCoordinate(raw: String): Coordinate {
    val parts = raw.split(':')
    require(parts.size == COORDINATE_PART_COUNT && parts.none { it.isBlank() }) {
        "Invalid --plugin value '$raw'. Expected group:artifact:version."
    }
    val group = parts[0]
    require(!group.startsWith("me.liam.microsmith")) {
        "Built-in Microsmith dependencies are pinned in the CLI distribution. " +
            "Use external plugin coordinates only."
    }

    val artifact = parts[1]
    val version = parts[2]
    validateCoordinateGroup(group)
    validateCoordinateSegment("artifact", artifact)
    validateCoordinateSegment("version", version)

    return Coordinate(group = group, artifact = artifact, version = version)
}

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
    val parent = normalizedScriptPath.parent
    return if (parent != null) {
        parent.resolve(lockfileName)
    } else {
        Path.of(lockfileName)
    }
}

internal fun sha256(path: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) {
                break
            }
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().toHexString()
}

internal fun isSha256(value: String): Boolean {
    return value.length == HEX_SHA256_LENGTH && value.matches(Regex("^[a-f0-9]+$"))
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }

private fun validateCoordinateGroup(group: String) {
    val segments = group.split('.')
    require(segments.none(String::isBlank)) {
        "Plugin coordinate group '$group' contains an empty package segment."
    }
    segments.forEach { segment ->
        validateCoordinateSegment("group", segment)
    }
}

private fun validateCoordinateSegment(
    label: String,
    value: String
) {
    require(!value.contains('/') && !value.contains('\\')) {
        "Plugin coordinate $label '$value' contains a path separator."
    }
    require(value != "." && value != "..") {
        "Plugin coordinate $label '$value' contains an invalid path segment."
    }
}
