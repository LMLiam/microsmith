package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Files
import java.nio.file.Path

internal sealed interface PluginResolutionResult {
    data class Success(
        val classpath: List<Path>,
        val lockfilePath: Path?
    ) : PluginResolutionResult

    data class Failure(
        val diagnostics: List<String>
    ) : PluginResolutionResult
}

internal data class PluginResolverSettings(
    val cacheDirectory: Path = defaultPluginCacheDirectory(),
    val lockfilePathOverride: Path? = null,
    val defaultRepositories: List<String> = listOf(MAVEN_CENTRAL_REPOSITORY)
)

internal fun resolvePlugins(
    command: RunCommand,
    settings: PluginResolverSettings = PluginResolverSettings()
): PluginResolutionResult =
    runCatching {
        resolvePluginsOrThrow(command, settings)
    }.getOrElse { error ->
        val message = error.message ?: error::class.simpleName ?: "unknown plugin resolution error"
        PluginResolutionResult.Failure(listOf(message))
    }

private fun resolvePluginsOrThrow(
    command: RunCommand,
    settings: PluginResolverSettings
): PluginResolutionResult.Success {
    if (command.plugins.isEmpty() && command.pluginJars.isEmpty()) {
        return PluginResolutionResult.Success(classpath = emptyList(), lockfilePath = null)
    }

    val coordinates = command.plugins.toList().sorted().map(::parseCoordinate)
    val localPluginJars =
        command.pluginJars
            .map { it.toAbsolutePath().normalize() }
            .sortedBy { it.toString() }
    localPluginJars.forEach { path ->
        require(Files.exists(path) && Files.isRegularFile(path)) {
            "Plugin jar '$path' does not exist or is not a file."
        }
    }

    val lockfilePath = settings.lockfilePathOverride ?: defaultLockfilePath(command.script)
    val lockfile = readLockfile(lockfilePath)
    lockfile?.assertSamePluginSet(buildRequestedLockKeys(coordinates, localPluginJars), lockfilePath)

    val repositories = resolveRepositories(command, settings)
    val cacheDirectory = settings.cacheDirectory.toAbsolutePath().normalize()
    Files.createDirectories(cacheDirectory)

    val classpath = mutableListOf<Path>()
    val lockEntries = mutableListOf<LockEntry>()

    coordinates.forEach { coordinate ->
        val artifactPath = resolveRemoteArtifact(coordinate, repositories, cacheDirectory, command.offline)
        val checksum = sha256(artifactPath)
        lockfile?.verifyChecksum(REMOTE_KIND, coordinate.value, checksum)
        lockEntries.add(LockEntry(kind = REMOTE_KIND, key = coordinate.value, checksum = checksum))
        classpath.add(artifactPath)
    }

    localPluginJars.forEach { path ->
        val checksum = sha256(path)
        val lockKey = path.toString()
        lockfile?.verifyChecksum(LOCAL_KIND, lockKey, checksum)
        lockEntries.add(LockEntry(kind = LOCAL_KIND, key = lockKey, checksum = checksum))
        classpath.add(path)
    }

    if (lockfile == null) {
        writeLockfile(
            lockfilePath = lockfilePath,
            lockfile =
                ParsedLockfile(
                    version = LOCKFILE_VERSION,
                    entries = lockEntries.sortedWith(compareBy(LockEntry::kind, LockEntry::key))
                )
        )
    }

    return PluginResolutionResult.Success(
        classpath = classpath.distinct(),
        lockfilePath = lockfilePath
    )
}

private fun buildRequestedLockKeys(
    coordinates: List<Coordinate>,
    localPluginJars: List<Path>
): Set<LockKey> {
    val remoteKeys = coordinates.map { LockKey(kind = REMOTE_KIND, key = it.value) }
    val localKeys = localPluginJars.map { LockKey(kind = LOCAL_KIND, key = it.toString()) }
    return (remoteKeys + localKeys).toSet()
}
