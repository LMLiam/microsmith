package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Files
import java.nio.file.Path

internal sealed interface PluginResolutionResult {
    data class Success(val classpath: List<Path>, val lockfilePath: Path?) : PluginResolutionResult

    data class Failure(val diagnostics: List<String>) : PluginResolutionResult
}

internal data class PluginResolverSettings(
    val cacheDirectory: Path = defaultPluginCacheDirectory(),
    val lockfilePathOverride: Path? = null,
    val defaultRepositories: List<String> = listOf(MAVEN_CENTRAL_REPOSITORY),
    val repositoryPolicy: RepositoryAllowlistPolicy? = null,
    val repositoryCredentialsResolver: RepositoryCredentialsResolver = defaultRepositoryCredentialsResolver(),
    val checksumAllowlist: PluginChecksumAllowlist? = null,
    val remotePluginResolver: RemotePluginResolver = MavenRemotePluginResolver(),
)

internal fun resolvePlugins(command: RunCommand, settings: PluginResolverSettings? = null): PluginResolutionResult {
    var sensitiveValues: Set<String> = emptySet()
    return runCatching {
        val effectiveSettings = settings ?: PluginResolverSettings()
        sensitiveValues = effectiveSettings.repositoryCredentialsResolver.sensitiveValues()
        resolvePluginsOrThrow(command, effectiveSettings)
    }.fold(
        onSuccess = { success -> success },
        onFailure = { error ->
            PluginResolutionResult.Failure(listOf(error.toResolutionDiagnostic(sensitiveValues)))
        },
    )
}

private fun resolvePluginsOrThrow(
    command: RunCommand,
    settings: PluginResolverSettings,
): PluginResolutionResult.Success {
    if (command.plugins.isEmpty() && command.pluginJars.isEmpty()) {
        return PluginResolutionResult.Success(classpath = emptyList(), lockfilePath = null)
    }

    val coordinates = command.plugins.toList().sorted().map(::parseCoordinate)
    val localPluginJars = resolveLocalPluginJars(command.pluginJars)
    localPluginJars.forEach { localJar ->
        require(Files.exists(localJar.artifactPath) && Files.isRegularFile(localJar.artifactPath)) {
            "Plugin jar '${localJar.artifactPath}' does not exist or is not a file."
        }
    }

    val lockfilePath = settings.lockfilePathOverride ?: defaultLockfilePath(command.script)
    val lockfile = readLockfile(lockfilePath)
    val repositoryPolicy = settings.repositoryPolicy ?: defaultRepositoryAllowlistPolicy()
    val repositoryCredentialsResolver = settings.repositoryCredentialsResolver
    val checksumAllowlist = settings.checksumAllowlist ?: loadPluginChecksumAllowlistFromEnvironment()
    val requestedLockKeys = buildRequestedLockKeys(coordinates, localPluginJars.map(LocalPluginJar::lockKey))
    checksumAllowlist?.assertCovers(requestedLockKeys)
    lockfile?.assertSamePluginSet(requestedLockKeys, lockfilePath)

    val repositories =
        try {
            resolveRepositoryEndpoints(command, settings, repositoryPolicy, repositoryCredentialsResolver)
        } catch (error: IllegalArgumentException) {
            throw PluginResolutionDiagnosticException(
                category = PluginResolverErrorCategory.REPOSITORY_POLICY,
                message = error.message ?: "Repository configuration was rejected by policy.",
                cause = error,
            )
        }
    val cacheDirectory = settings.cacheDirectory.toAbsolutePath().normalize()
    Files.createDirectories(cacheDirectory)

    val classpath = mutableListOf<Path>()
    val lockEntries = mutableListOf<LockEntry>()

    coordinates.forEach { coordinate ->
        val resolvedRemotePlugin =
            settings.remotePluginResolver.resolve(
                coordinate = coordinate,
                repositories = repositories,
                cacheDirectory = cacheDirectory,
                offline = command.offline,
            )
        val checksum = sha256(resolvedRemotePlugin.rootArtifactPath)
        lockfile?.verifyChecksum(REMOTE_KIND, coordinate.value, checksum)
        checksumAllowlist?.verifyChecksum(REMOTE_KIND, coordinate.value, checksum)
        lockEntries.add(LockEntry(kind = REMOTE_KIND, key = coordinate.value, checksum = checksum))
        classpath.addAll(resolvedRemotePlugin.classpath)
    }

    localPluginJars.forEach { localJar ->
        val checksum = sha256(localJar.artifactPath)
        lockfile?.verifyChecksum(LOCAL_KIND, localJar.lockKey, checksum)
        checksumAllowlist?.verifyChecksum(LOCAL_KIND, localJar.lockKey, checksum)
        lockEntries.add(LockEntry(kind = LOCAL_KIND, key = localJar.lockKey, checksum = checksum))
        classpath.add(localJar.artifactPath)
    }

    if (lockfile == null) {
        writeLockfile(
            lockfilePath = lockfilePath,
            lockfile =
            ParsedLockfile(
                version = LOCKFILE_VERSION,
                entries = lockEntries.sortedWith(compareBy(LockEntry::kind, LockEntry::key)),
            ),
        )
    }

    return PluginResolutionResult.Success(
        classpath = normalizeClasspath(classpath),
        lockfilePath = lockfilePath,
    )
}

private fun buildRequestedLockKeys(coordinates: List<Coordinate>, localPluginLockKeys: List<String>): Set<LockKey> {
    val remoteKeys = coordinates.map { coordinate -> LockKey(kind = REMOTE_KIND, key = coordinate.value) }
    val localKeys = localPluginLockKeys.map { lockKey -> LockKey(kind = LOCAL_KIND, key = lockKey) }
    return (remoteKeys + localKeys).toSet()
}

private fun localPluginLockKey(pluginJarPath: Path): String = pluginJarPath
    .normalize()
    .toString()
    .replace('\\', '/')

private fun resolveLocalPluginJars(pluginJars: Set<Path>): List<LocalPluginJar> = pluginJars
    .map { requestedPath ->
        LocalPluginJar(
            artifactPath = requestedPath.toAbsolutePath().normalize(),
            lockKey = localPluginLockKey(requestedPath),
        )
    }.sortedBy(LocalPluginJar::lockKey)

private fun Throwable.toResolutionDiagnostic(sensitiveValues: Set<String>): String = when (this) {
    is PluginResolutionDiagnosticException ->
        "[${category.code}] ${(message ?: "plugin resolution failed").redactSensitiveValues(sensitiveValues)}"

    else -> {
        val unexpectedMessage = message ?: this::class.simpleName ?: "unknown plugin resolution error"
        "[unexpected] ${unexpectedMessage.redactSensitiveValues(sensitiveValues)}"
    }
}

private fun normalizeClasspath(rawClasspath: List<Path>): List<Path> = rawClasspath
    .map { it.toAbsolutePath().normalize() }
    .distinct()

private data class LocalPluginJar(val artifactPath: Path, val lockKey: String)
