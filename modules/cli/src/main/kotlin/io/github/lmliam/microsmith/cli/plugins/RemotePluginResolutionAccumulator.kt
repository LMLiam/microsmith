package io.github.lmliam.microsmith.cli.plugins

import java.nio.file.Path

internal class RemotePluginResolutionAccumulator(
    private val checksumCalculator: (Path) -> String = ::sha256,
) {
    fun resolve(
        coordinates: List<Coordinate>,
        offline: Boolean,
        context: PluginResolutionContext,
        remotePluginResolver: RemotePluginResolver,
        integrityVerifier: PluginResolutionIntegrityVerifier,
    ): RemotePluginResolution {
        val classpath = mutableListOf<Path>()
        val rootLockEntries = mutableListOf<LockEntry>()
        val remoteArtifactChecksums = linkedMapOf<String, String>()

        coordinates.forEach { coordinate ->
            val resolvedRemotePlugin =
                remotePluginResolver.resolve(
                    coordinate = coordinate,
                    repositories = context.repositories,
                    cacheDirectory = context.cacheDirectory,
                    offline = offline,
                )
            val rootChecksum = checksumCalculator(resolvedRemotePlugin.rootArtifactPath)
            integrityVerifier.verifyChecksum(
                kind = REMOTE_KIND,
                key = coordinate.value,
                actualChecksum = rootChecksum,
                lockfile = context.lockfile,
                checksumAllowlist = context.checksumAllowlist,
            )
            rootLockEntries.add(LockEntry(kind = REMOTE_KIND, key = coordinate.value, checksum = rootChecksum))
            mergeRemoteArtifactChecksums(
                checksums = remoteArtifactChecksums,
                artifacts = resolvedRemotePlugin.artifacts,
                lockfile = context.lockfile,
                checksumAllowlist = context.checksumAllowlist,
                integrityVerifier = integrityVerifier,
            )
            classpath.addAll(resolvedRemotePlugin.classpath)
        }

        return RemotePluginResolution(
            classpath = classpath,
            rootLockEntries = rootLockEntries,
            remoteArtifactChecksums = remoteArtifactChecksums,
        )
    }

    private fun mergeRemoteArtifactChecksums(
        checksums: MutableMap<String, String>,
        artifacts: List<ResolvedRemoteArtifact>,
        lockfile: ParsedLockfile?,
        checksumAllowlist: PluginChecksumAllowlist?,
        integrityVerifier: PluginResolutionIntegrityVerifier,
    ) {
        artifacts.forEach { artifact ->
            val checksum = checksumCalculator(artifact.artifactPath)
            integrityVerifier.verifyChecksum(
                kind = REMOTE_ARTIFACT_KIND,
                key = artifact.lockKey,
                actualChecksum = checksum,
                lockfile = lockfile,
                checksumAllowlist = checksumAllowlist,
            )
            val previous = checksums.putIfAbsent(artifact.lockKey, checksum)
            require(previous == null || previous == checksum) {
                "Resolved remote artifact '${artifact.lockKey}' produced inconsistent checksums."
            }
        }
    }
}
