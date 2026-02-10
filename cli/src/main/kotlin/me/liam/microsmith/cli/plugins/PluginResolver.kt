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
            .map { requestedPath ->
                LocalPluginJar(
                    artifactPath = requestedPath.toAbsolutePath().normalize(),
                    lockKey = localPluginLockKey(requestedPath)
                )
            }.sortedBy(LocalPluginJar::lockKey)
    localPluginJars.forEach { localJar ->
        require(Files.exists(localJar.artifactPath) && Files.isRegularFile(localJar.artifactPath)) {
            "Plugin jar '${localJar.artifactPath}' does not exist or is not a file."
        }
    }

    val lockfilePath = settings.lockfilePathOverride ?: defaultLockfilePath(command.script)
    val lockfile = readLockfile(lockfilePath)
    lockfile?.assertSamePluginSet(
        buildRequestedLockKeys(coordinates, localPluginJars.map(LocalPluginJar::lockKey)),
        lockfilePath
    )

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

    localPluginJars.forEach { localJar ->
        val checksum = sha256(localJar.artifactPath)
        lockfile?.verifyChecksum(LOCAL_KIND, localJar.lockKey, checksum)
        lockEntries.add(LockEntry(kind = LOCAL_KIND, key = localJar.lockKey, checksum = checksum))
        classpath.add(localJar.artifactPath)
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
    localPluginLockKeys: List<String>
): Set<LockKey> {
    val remoteKeys = coordinates.map { LockKey(kind = REMOTE_KIND, key = it.value) }
    val localKeys = localPluginLockKeys.map { LockKey(kind = LOCAL_KIND, key = it) }
    return (remoteKeys + localKeys).toSet()
}

private fun localPluginLockKey(pluginJarPath: Path): String =
    pluginJarPath
        .normalize()
        .toString()
        .replace('\\', '/')

private data class LocalPluginJar(
    val artifactPath: Path,
    val lockKey: String
)
