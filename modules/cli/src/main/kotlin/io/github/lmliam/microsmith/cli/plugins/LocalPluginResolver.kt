package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Files
import java.nio.file.Path

internal class LocalPluginResolver(private val checksumCalculator: (Path) -> String = ::sha256) {
    fun resolveValidated(pluginJars: Set<Path>): List<LocalPluginJar> {
        val localPluginJars =
            pluginJars
                .map { requestedPath ->
                    LocalPluginJar(
                        artifactPath = requestedPath.toAbsolutePath().normalize(),
                        lockKey = requestedPath.toLocalPluginLockKey(),
                    )
                }.sortedBy(LocalPluginJar::lockKey)
        localPluginJars.forEach { localJar ->
            require(Files.exists(localJar.artifactPath) && Files.isRegularFile(localJar.artifactPath)) {
                "Plugin jar '${localJar.artifactPath}' does not exist or is not a file."
            }
        }
        return localPluginJars
    }

    fun resolve(
        localPluginJars: List<LocalPluginJar>,
        integrityVerifier: PluginResolutionIntegrityVerifier,
        lockfile: ParsedLockfile?,
        checksumAllowlist: PluginChecksumAllowlist?,
    ): LocalPluginResolution {
        val classpath = mutableListOf<Path>()
        val lockEntries = mutableListOf<LockEntry>()

        localPluginJars.forEach { localJar ->
            val checksum = checksumCalculator(localJar.artifactPath)
            integrityVerifier.verifyChecksum(
                kind = LOCAL_KIND,
                key = localJar.lockKey,
                actualChecksum = checksum,
                lockfile = lockfile,
                checksumAllowlist = checksumAllowlist,
            )
            lockEntries.add(LockEntry(kind = LOCAL_KIND, key = localJar.lockKey, checksum = checksum))
            classpath.add(localJar.artifactPath)
        }

        return LocalPluginResolution(classpath = classpath, lockEntries = lockEntries)
    }
}

private fun Path.toLocalPluginLockKey(): String = normalize().toString().replace('\\', '/')
