package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path

/** Offline resolution requires a complete graph lock and matching cached jars and descriptors. */
internal class OfflinePluginCacheReadinessChecker {
    fun assertReady(lockfile: ParsedLockfile?, lockfilePath: Path, cacheRoot: Path) {
        val failure = offlineCacheFailure(lockfile, lockfilePath, cacheRoot) ?: return
        throw failure
    }

    private fun findMissingArtifacts(entries: List<LockEntry>, cacheRoot: Path): List<String> = entries
        .map { entry -> entry.key to cacheRoot.resolve(entry.key).normalize() }
        .mapNotNull { (key, path) ->
            when {
                !path.startsWith(cacheRoot) -> key
                !Files.exists(path) || !Files.isRegularFile(path) -> key
                else -> null
            }
        }

    private fun offlineCacheFailure(
        lockfile: ParsedLockfile?,
        lockfilePath: Path,
        cacheRoot: Path,
    ): PluginResolutionDiagnosticException? {
        if (lockfile == null) {
            return offlineCacheMiss(
                "Offline mode requires a plugin lockfile. " +
                    "Generate '$lockfilePath' by running once without --offline.",
            )
        }

        val remoteArtifactEntries = lockfile.entries.filter { entry -> entry.kind == REMOTE_ARTIFACT_KIND }
        if (remoteArtifactEntries.isEmpty()) {
            return offlineCacheMiss(
                "Offline mode requires locked remote dependency graph entries in '$lockfilePath'. " +
                    "Regenerate the lockfile by running once without --offline.",
            )
        }

        val missingArtifacts = findMissingArtifacts(remoteArtifactEntries, cacheRoot)
        if (missingArtifacts.isEmpty()) {
            return null
        }

        return offlineCacheMiss(
            "Offline mode is enabled but plugin cache is missing locked dependency graph artifacts: " +
                missingArtifacts.sorted().joinToString(", ") +
                ". Run once without --offline to restore the cache.",
        )
    }

    private fun offlineCacheMiss(message: String): PluginResolutionDiagnosticException =
        PluginResolutionDiagnosticException(
            category = PluginResolverErrorCategory.OFFLINE_CACHE_MISS,
            message = message,
        )
}
