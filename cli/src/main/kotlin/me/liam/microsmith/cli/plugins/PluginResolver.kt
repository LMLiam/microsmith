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
    val repositoryCredentialsResolver: RepositoryCredentialsResolver = lazyDefaultRepositoryCredentialsResolver(),
    val checksumAllowlist: PluginChecksumAllowlist? = null,
    val remotePluginResolver: RemotePluginResolver = MavenRemotePluginResolver(),
)

internal fun resolvePlugins(command: RunCommand): PluginResolutionResult {
    if (command.plugins.isEmpty() && command.pluginJars.isEmpty()) {
        return PluginResolutionResult.Success(classpath = emptyList(), lockfilePath = null)
    }

    return runCatching {
        PluginResolverSettings()
    }.fold(
        onSuccess = { settings -> resolvePlugins(command = command, settings = settings) },
        onFailure = { error ->
            PluginResolutionResult.Failure(listOf(error.toResolutionDiagnostic(emptySet())))
        },
    )
}

internal fun resolvePlugins(command: RunCommand, settings: PluginResolverSettings): PluginResolutionResult {
    if (command.plugins.isEmpty() && command.pluginJars.isEmpty()) {
        return PluginResolutionResult.Success(classpath = emptyList(), lockfilePath = null)
    }

    var sensitiveValues: Set<String> = emptySet()
    return runCatching {
        if (command.plugins.isNotEmpty()) {
            sensitiveValues = settings.repositoryCredentialsResolver.sensitiveValues()
        }
        resolvePluginsOrThrow(command, settings)
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
    val localPluginJars = resolveAndValidateLocalPluginJars(command.pluginJars)
    val context =
        buildResolutionContext(
            command = command,
            settings = settings,
            coordinates = coordinates,
            localPluginJars = localPluginJars,
        )
    Files.createDirectories(context.cacheDirectory)

    val remoteResolution =
        resolveRemotePlugins(
            coordinates = coordinates,
            command = command,
            context = context,
            settings = settings,
        )
    val localResolution =
        resolveLocalPlugins(
            localPluginJars = localPluginJars,
            lockfile = context.lockfile,
            checksumAllowlist = context.checksumAllowlist,
        )

    withLockfileDiagnostics {
        context.lockfile?.assertSameRemoteArtifactSet(
            remoteResolution.remoteArtifactChecksums.keys.toSet(),
            context.lockfilePath,
        )
    }

    val lockEntries =
        buildList {
            addAll(remoteResolution.rootRemoteLockEntries)
            addAll(remoteResolution.remoteArtifactChecksums.toLockEntries())
            addAll(localResolution.lockEntries)
        }.sortedWith(compareBy(LockEntry::kind, LockEntry::key))

    context.checksumAllowlist?.assertCovers(lockEntries.toLockKeys())

    if (context.lockfile == null) {
        writeLockfile(
            lockfilePath = context.lockfilePath,
            lockfile =
            ParsedLockfile(
                version = LOCKFILE_VERSION,
                entries = lockEntries,
            ),
        )
    }

    return PluginResolutionResult.Success(
        classpath = normalizeClasspath(remoteResolution.classpath + localResolution.classpath),
        lockfilePath = context.lockfilePath,
    )
}

private fun buildResolutionContext(
    command: RunCommand,
    settings: PluginResolverSettings,
    coordinates: List<Coordinate>,
    localPluginJars: List<LocalPluginJar>,
): ResolutionContext {
    val lockfilePath = settings.lockfilePathOverride ?: defaultLockfilePath(command.script)
    val lockfile = withLockfileDiagnostics { readLockfile(lockfilePath) }
    val checksumAllowlist = settings.checksumAllowlist ?: loadPluginChecksumAllowlistFromEnvironment()
    val requestedLockKeys = buildRequestedLockKeys(coordinates, localPluginJars.map(LocalPluginJar::lockKey))
    checksumAllowlist?.assertCovers(requestedLockKeys)
    withLockfileDiagnostics { lockfile?.assertSamePluginSet(requestedLockKeys, lockfilePath) }

    val cacheDirectory = settings.cacheDirectory.toAbsolutePath().normalize()
    if (command.offline && coordinates.isNotEmpty()) {
        assertOfflineGraphReadiness(
            lockfile = lockfile,
            lockfilePath = lockfilePath,
            cacheRoot = pluginArtifactCacheRoot(cacheDirectory),
        )
    }

    return ResolutionContext(
        lockfilePath = lockfilePath,
        lockfile = lockfile,
        checksumAllowlist = checksumAllowlist,
        cacheDirectory = cacheDirectory,
        repositories = resolveRepositoryEndpointsForCommand(command, settings, coordinates),
    )
}

private fun resolveRepositoryEndpointsForCommand(
    command: RunCommand,
    settings: PluginResolverSettings,
    coordinates: List<Coordinate>,
): List<RepositoryEndpoint> {
    if (coordinates.isEmpty()) {
        return emptyList()
    }

    return try {
        val repositoryPolicy = settings.repositoryPolicy ?: defaultRepositoryAllowlistPolicy()
        resolveRepositoryEndpoints(command, settings, repositoryPolicy, settings.repositoryCredentialsResolver)
    } catch (error: IllegalArgumentException) {
        throw PluginResolutionDiagnosticException(
            category = PluginResolverErrorCategory.REPOSITORY_POLICY,
            message = error.message ?: "Repository configuration was rejected by policy.",
            cause = error,
        )
    }
}

private fun resolveRemotePlugins(
    coordinates: List<Coordinate>,
    command: RunCommand,
    context: ResolutionContext,
    settings: PluginResolverSettings,
): RemoteResolutionSummary {
    val classpath = mutableListOf<Path>()
    val rootRemoteLockEntries = mutableListOf<LockEntry>()
    val remoteArtifactChecksums = linkedMapOf<String, String>()

    coordinates.forEach { coordinate ->
        val resolvedRemotePlugin =
            settings.remotePluginResolver.resolve(
                coordinate = coordinate,
                repositories = context.repositories,
                cacheDirectory = context.cacheDirectory,
                offline = command.offline,
            )
        val rootChecksum = sha256(resolvedRemotePlugin.rootArtifactPath)
        withLockfileDiagnostics {
            context.lockfile?.verifyChecksum(REMOTE_KIND, coordinate.value, rootChecksum)
        }
        context.checksumAllowlist?.verifyChecksum(REMOTE_KIND, coordinate.value, rootChecksum)
        rootRemoteLockEntries.add(LockEntry(kind = REMOTE_KIND, key = coordinate.value, checksum = rootChecksum))
        mergeRemoteArtifactChecksums(
            checksums = remoteArtifactChecksums,
            artifacts = resolvedRemotePlugin.artifacts,
            lockfile = context.lockfile,
            checksumAllowlist = context.checksumAllowlist,
        )
        classpath.addAll(resolvedRemotePlugin.classpath)
    }

    return RemoteResolutionSummary(
        classpath = classpath,
        rootRemoteLockEntries = rootRemoteLockEntries,
        remoteArtifactChecksums = remoteArtifactChecksums,
    )
}

private fun mergeRemoteArtifactChecksums(
    checksums: MutableMap<String, String>,
    artifacts: List<ResolvedRemoteArtifact>,
    lockfile: ParsedLockfile?,
    checksumAllowlist: PluginChecksumAllowlist?,
) {
    artifacts.forEach { artifact ->
        val checksum = sha256(artifact.artifactPath)
        withLockfileDiagnostics {
            lockfile?.verifyChecksum(REMOTE_ARTIFACT_KIND, artifact.lockKey, checksum)
        }
        checksumAllowlist?.verifyChecksum(REMOTE_ARTIFACT_KIND, artifact.lockKey, checksum)
        val previous = checksums.putIfAbsent(artifact.lockKey, checksum)
        require(previous == null || previous == checksum) {
            "Resolved remote artifact '${artifact.lockKey}' produced inconsistent checksums."
        }
    }
}

private fun resolveLocalPlugins(
    localPluginJars: List<LocalPluginJar>,
    lockfile: ParsedLockfile?,
    checksumAllowlist: PluginChecksumAllowlist?,
): LocalResolutionSummary {
    val classpath = mutableListOf<Path>()
    val lockEntries = mutableListOf<LockEntry>()

    localPluginJars.forEach { localJar ->
        val checksum = sha256(localJar.artifactPath)
        withLockfileDiagnostics {
            lockfile?.verifyChecksum(LOCAL_KIND, localJar.lockKey, checksum)
        }
        checksumAllowlist?.verifyChecksum(LOCAL_KIND, localJar.lockKey, checksum)
        lockEntries.add(LockEntry(kind = LOCAL_KIND, key = localJar.lockKey, checksum = checksum))
        classpath.add(localJar.artifactPath)
    }

    return LocalResolutionSummary(classpath = classpath, lockEntries = lockEntries)
}

private fun buildRequestedLockKeys(coordinates: List<Coordinate>, localPluginLockKeys: List<String>): Set<LockKey> {
    val remoteKeys = coordinates.map { coordinate -> LockKey(kind = REMOTE_KIND, key = coordinate.value) }
    val localKeys = localPluginLockKeys.map { lockKey -> LockKey(kind = LOCAL_KIND, key = lockKey) }
    return (remoteKeys + localKeys).toSet()
}

private fun assertOfflineGraphReadiness(lockfile: ParsedLockfile?, lockfilePath: Path, cacheRoot: Path) {
    val errorMessage =
        when {
            lockfile == null ->
                "Offline mode requires a plugin lockfile. Generate '$lockfilePath' by running once without --offline."

            else -> {
                val remoteArtifactEntries = lockfile.entries.filter { entry -> entry.kind == REMOTE_ARTIFACT_KIND }
                when {
                    remoteArtifactEntries.isEmpty() ->
                        "Offline mode requires locked remote dependency graph entries in '$lockfilePath'. " +
                            "Regenerate the lockfile by running once without --offline."

                    else -> {
                        val missingArtifacts = findMissingOfflineArtifacts(remoteArtifactEntries, cacheRoot)
                        if (missingArtifacts.isNotEmpty()) {
                            "Offline mode is enabled but plugin cache is missing locked dependency graph artifacts: " +
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
        failOfflineCacheReadiness(errorMessage)
    }
}

private fun findMissingOfflineArtifacts(entries: List<LockEntry>, cacheRoot: Path): List<String> = entries
    .map { entry -> entry.key to cacheRoot.resolve(entry.key).normalize() }
    .mapNotNull { (key, path) ->
        when {
            !path.startsWith(cacheRoot) -> key
            !Files.exists(path) || !Files.isRegularFile(path) -> key
            else -> null
        }
    }

private fun failOfflineCacheReadiness(message: String): Nothing = throw PluginResolutionDiagnosticException(
    category = PluginResolverErrorCategory.OFFLINE_CACHE_MISS,
    message = message,
)

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

private fun resolveAndValidateLocalPluginJars(pluginJars: Set<Path>): List<LocalPluginJar> {
    val localPluginJars = resolveLocalPluginJars(pluginJars)
    localPluginJars.forEach { localJar ->
        require(Files.exists(localJar.artifactPath) && Files.isRegularFile(localJar.artifactPath)) {
            "Plugin jar '${localJar.artifactPath}' does not exist or is not a file."
        }
    }
    return localPluginJars
}

private inline fun <T> withLockfileDiagnostics(block: () -> T): T = try {
    block()
} catch (error: IllegalArgumentException) {
    throw PluginResolutionDiagnosticException(
        category = PluginResolverErrorCategory.LOCKFILE,
        message = error.message ?: "Plugin lockfile validation failed.",
        cause = error,
    )
}

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

private fun Map<String, String>.toLockEntries(): List<LockEntry> = entries.map { (key, checksum) ->
    LockEntry(kind = REMOTE_ARTIFACT_KIND, key = key, checksum = checksum)
}

private fun List<LockEntry>.toLockKeys(): Set<LockKey> = map { entry ->
    LockKey(kind = entry.kind, key = entry.key)
}.toSet()

private data class ResolutionContext(
    val lockfilePath: Path,
    val lockfile: ParsedLockfile?,
    val checksumAllowlist: PluginChecksumAllowlist?,
    val cacheDirectory: Path,
    val repositories: List<RepositoryEndpoint>,
)

private data class RemoteResolutionSummary(
    val classpath: List<Path>,
    val rootRemoteLockEntries: List<LockEntry>,
    val remoteArtifactChecksums: Map<String, String>,
)

private data class LocalResolutionSummary(val classpath: List<Path>, val lockEntries: List<LockEntry>)

private data class LocalPluginJar(val artifactPath: Path, val lockKey: String)
