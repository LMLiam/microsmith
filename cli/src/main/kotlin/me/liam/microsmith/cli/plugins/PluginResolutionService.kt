package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Files

internal class PluginResolutionService(
    private val settings: PluginResolverSettings,
    private val contextFactory: PluginResolutionContextFactory = PluginResolutionContextFactory(),
    private val integrityVerifier: PluginResolutionIntegrityVerifier = PluginResolutionIntegrityVerifier(),
    private val remoteResolutionAccumulator: RemotePluginResolutionAccumulator = RemotePluginResolutionAccumulator(),
    private val localPluginResolver: LocalPluginResolver = LocalPluginResolver(),
) {
    fun resolve(command: RunCommand): PluginResolutionResult.Success {
        val coordinates = command.plugins.toList().sorted().map(::parseCoordinate)
        val localPluginJars = localPluginResolver.resolveValidated(command.pluginJars)
        val context =
            contextFactory.create(
                command = command,
                settings = settings,
                coordinates = coordinates,
                localPluginJars = localPluginJars,
            )
        Files.createDirectories(context.cacheDirectory)

        val remoteResolution =
            remoteResolutionAccumulator.resolve(
                coordinates = coordinates,
                offline = command.offline,
                context = context,
                remotePluginResolver = settings.remotePluginResolver,
                integrityVerifier = integrityVerifier,
            )
        val localResolution =
            localPluginResolver.resolve(
                localPluginJars = localPluginJars,
                integrityVerifier = integrityVerifier,
                lockfile = context.lockfile,
                checksumAllowlist = context.checksumAllowlist,
            )

        integrityVerifier.assertSameRemoteArtifactSet(
            lockfile = context.lockfile,
            resolvedKeys = remoteResolution.remoteArtifactChecksums.keys.toSet(),
            lockfilePath = context.lockfilePath,
        )

        val lockEntries =
            buildList {
                addAll(remoteResolution.rootLockEntries)
                addAll(remoteResolution.remoteArtifactChecksums.toRemoteArtifactLockEntries())
                addAll(localResolution.lockEntries)
            }.sortedWith(compareBy(LockEntry::kind, LockEntry::key))

        integrityVerifier.assertAllowlistCoverage(context.checksumAllowlist, lockEntries)
        integrityVerifier.writeGeneratedLockfile(
            lockfilePath = context.lockfilePath,
            existingLockfile = context.lockfile,
            lockEntries = lockEntries,
        )

        return PluginResolutionResult.Success(
            classpath = (remoteResolution.classpath + localResolution.classpath).normalizePluginClasspath(),
            lockfilePath = context.lockfilePath,
        )
    }
}
