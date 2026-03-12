package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path

private const val LOCKFILE_ENTRY_PARTS = 3

internal fun readLockfile(lockfilePath: Path): ParsedLockfile? {
    if (!Files.exists(lockfilePath)) {
        return null
    }

    val nonBlankLines = Files.readAllLines(lockfilePath).map(String::trim).filter(String::isNotEmpty)
    require(nonBlankLines.isNotEmpty()) {
        "Plugin lockfile '$lockfilePath' is empty."
    }

    val versionLine = nonBlankLines.first()
    require(versionLine.startsWith("version=")) {
        "Plugin lockfile '$lockfilePath' is invalid. Missing version line."
    }
    val version = versionLine.substringAfter("version=").toIntOrNull()
    require(version == LOCKFILE_VERSION) {
        "Plugin lockfile '$lockfilePath' has unsupported version '$version'. Expected '$LOCKFILE_VERSION'."
    }

    val entries =
        nonBlankLines
            .drop(1)
            .map(::parseLockEntry)
            .distinctBy { it.kind to it.key }

    return ParsedLockfile(version = version, entries = entries)
}

internal fun ParsedLockfile.assertSamePluginSet(requestedKeys: Set<LockKey>, lockfilePath: Path) {
    val lockedKeys =
        entries
            .asSequence()
            .filter(::isRequestedPluginEntry)
            .map { entry -> LockKey(kind = entry.kind, key = entry.key) }
            .toSet()
    val missingFromLock = requestedKeys - lockedKeys
    val extraInLock = lockedKeys - requestedKeys

    require(missingFromLock.isEmpty() && extraInLock.isEmpty()) {
        buildString {
            append("Plugin set does not match lockfile '$lockfilePath'.")
            if (missingFromLock.isNotEmpty()) {
                append(" Missing from lockfile: ${missingFromLock.joinToString { "${it.kind}:${it.key}" }}.")
            }
            if (extraInLock.isNotEmpty()) {
                append(" Not requested by CLI: ${extraInLock.joinToString { "${it.kind}:${it.key}" }}.")
            }
            append(" Update plugin flags or regenerate the lockfile.")
        }
    }
}

internal fun ParsedLockfile.assertSameRemoteArtifactSet(resolvedKeys: Set<String>, lockfilePath: Path) {
    val lockedKeys =
        entries
            .asSequence()
            .filter { entry -> entry.kind == REMOTE_ARTIFACT_KIND }
            .map(LockEntry::key)
            .toSet()
    val missingFromLock = resolvedKeys - lockedKeys
    val extraInLock = lockedKeys - resolvedKeys

    require(missingFromLock.isEmpty() && extraInLock.isEmpty()) {
        buildString {
            append("Resolved remote dependency graph does not match lockfile '$lockfilePath'.")
            if (missingFromLock.isNotEmpty()) {
                append(" Missing from lockfile: ${missingFromLock.sorted().joinToString()}.")
            }
            if (extraInLock.isNotEmpty()) {
                append(" Not present in resolved graph: ${extraInLock.sorted().joinToString()}.")
            }
            append(" Regenerate the lockfile after reviewing dependency changes.")
        }
    }
}

internal fun ParsedLockfile.verifyChecksum(kind: String, key: String, actualChecksum: String) {
    val expected =
        entries
            .firstOrNull { it.kind == kind && it.key == key }
            ?.checksum
            ?: return

    require(expected == actualChecksum) {
        "Checksum mismatch for $kind plugin '$key'. Expected '$expected' but found '$actualChecksum'."
    }
}

internal fun writeLockfile(lockfilePath: Path, lockfile: ParsedLockfile) {
    val lines = buildList {
        add("version=${lockfile.version}")
        lockfile.entries
            .sortedWith(compareBy(LockEntry::kind, LockEntry::key))
            .forEach { entry ->
                add("${entry.kind}|${entry.key}|${entry.checksum}")
            }
    }

    lockfilePath.parent?.let(Files::createDirectories)
    Files.write(lockfilePath, lines)
}

private fun parseLockEntry(line: String): LockEntry {
    val parts = line.split('|')
    require(parts.size == LOCKFILE_ENTRY_PARTS) {
        "Invalid plugin lockfile entry '$line'. Expected <kind>|<key>|<sha256>."
    }

    val kind = parts[0]
    val key = parts[1]
    val checksum = parts[2]
    val allowedKinds = setOf(REMOTE_KIND, REMOTE_ARTIFACT_KIND, LOCAL_KIND)
    require(kind in allowedKinds) {
        "Invalid plugin lockfile entry kind '$kind'."
    }
    require(key.isNotBlank()) { "Plugin lockfile entry key must not be blank." }
    require(isSha256(checksum)) {
        "Plugin lockfile checksum for '$key' is invalid."
    }

    return LockEntry(kind = kind, key = key, checksum = checksum)
}

private fun isRequestedPluginEntry(entry: LockEntry): Boolean = entry.kind == REMOTE_KIND || entry.kind == LOCAL_KIND
