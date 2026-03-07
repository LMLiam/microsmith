package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand

internal class PluginResolutionContextFactory(
    private val integrityVerifier: PluginResolutionIntegrityVerifier = PluginResolutionIntegrityVerifier(),
    private val repositoryResolver: PluginRepositoryResolver = PluginRepositoryResolver(),
    private val offlineCacheReadinessChecker: OfflinePluginCacheReadinessChecker = OfflinePluginCacheReadinessChecker(),
) {
    fun create(
        command: RunCommand,
        settings: PluginResolverSettings,
        coordinates: List<Coordinate>,
        localPluginJars: List<LocalPluginJar>,
    ): PluginResolutionContext {
        val lockfilePath = settings.lockfilePathOverride ?: defaultLockfilePath(command.script)
        val lockfile = integrityVerifier.loadLockfile(lockfilePath)
        val checksumAllowlist = settings.checksumAllowlist ?: loadPluginChecksumAllowlistFromEnvironment()
        val requestedLockKeys =
            buildRequestedLockKeys(
                coordinates = coordinates,
                localPluginLockKeys = localPluginJars.map(LocalPluginJar::lockKey),
            )
        integrityVerifier.assertAllowlistCoverage(checksumAllowlist, requestedLockKeys)
        integrityVerifier.assertSamePluginSet(lockfile, requestedLockKeys, lockfilePath)

        val cacheDirectory = settings.cacheDirectory.toAbsolutePath().normalize()
        if (command.offline && coordinates.isNotEmpty()) {
            offlineCacheReadinessChecker.assertReady(
                lockfile = lockfile,
                lockfilePath = lockfilePath,
                cacheRoot = pluginArtifactCacheRoot(cacheDirectory),
            )
        }

        return PluginResolutionContext(
            lockfilePath = lockfilePath,
            lockfile = lockfile,
            checksumAllowlist = checksumAllowlist,
            cacheDirectory = cacheDirectory,
            repositories =
            repositoryResolver.resolve(
                command = command,
                settings = settings,
                requiresRemoteRepositories = coordinates.isNotEmpty(),
            ),
        )
    }

    private fun buildRequestedLockKeys(
        coordinates: List<Coordinate>,
        localPluginLockKeys: List<String>,
    ): Set<LockKey> {
        val remoteKeys = coordinates.map { coordinate -> LockKey(kind = REMOTE_KIND, key = coordinate.value) }
        val localKeys = localPluginLockKeys.map { lockKey -> LockKey(kind = LOCAL_KIND, key = lockKey) }
        return (remoteKeys + localKeys).toSet()
    }
}
