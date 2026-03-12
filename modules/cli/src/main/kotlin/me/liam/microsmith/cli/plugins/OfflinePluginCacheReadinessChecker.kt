package me.liam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path

/** Offline resolution requires a complete graph lock and matching cached jars and descriptors. */
internal class OfflinePluginCacheReadinessChecker {
    fun assertReady(lockfile: ParsedLockfile?, lockfilePath: Path, cacheRoot: Path) {
        val errorMessage =
            when {
                lockfile == null ->
                    "Offline mode requires a plugin lockfile. " +
                        "Generate '$lockfilePath' by running once without --offline."

                else -> {
                    val remoteArtifactEntries = lockfile.entries.filter { entry -> entry.kind == REMOTE_ARTIFACT_KIND }
                    when {
                        remoteArtifactEntries.isEmpty() ->
                            "Offline mode requires locked remote dependency graph entries in '$lockfilePath'. " +
                                "Regenerate the lockfile by running once without --offline."

                        else -> {
                            val missingArtifacts = findMissingArtifacts(remoteArtifactEntries, cacheRoot)
                            if (missingArtifacts.isNotEmpty()) {
                                "Offline mode is enabled but plugin cache is missing " +
                                    "locked dependency graph artifacts: " +
                                    missingArtifacts.sorted().joinToString(", ") +
                                    ". Run once without --offline to restore the cache."
                            } else {
                                null
                            }
                        }
                    }
                }
            }

        if (errorMessage != null) {
            throw PluginResolutionDiagnosticException(
                category = PluginResolverErrorCategory.OFFLINE_CACHE_MISS,
                message = errorMessage,
            )
        }
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
}
