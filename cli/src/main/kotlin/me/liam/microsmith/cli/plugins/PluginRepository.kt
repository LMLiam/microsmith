package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand
import java.nio.file.Path

internal fun resolveRepositories(command: RunCommand, settings: PluginResolverSettings): List<String> =
    resolveRepositories(command, settings, settings.repositoryPolicy ?: defaultRepositoryAllowlistPolicy())

internal fun resolveRepositories(
    command: RunCommand,
    settings: PluginResolverSettings,
    repositoryPolicy: RepositoryAllowlistPolicy,
): List<String> {
    val override = command.repositoryOverride?.trim()?.takeIf { it.isNotEmpty() }
    val repositories = (listOfNotNull(override) + settings.defaultRepositories).map(::normalizeRepositoryUri).distinct()
    repositories.forEach(repositoryPolicy::validate)
    return repositories
}

internal fun pluginArtifactCacheRoot(cacheDirectory: Path): Path = cacheDirectory
    .resolve("artifacts")
    .toAbsolutePath()
    .normalize()

internal fun cachePathFor(cacheRoot: Path, coordinate: Coordinate): Path {
    val artifactPath =
        cacheRoot
            .resolve(coordinate.group.replace('.', '/'))
            .resolve(coordinate.artifact)
            .resolve(coordinate.version)
            .resolve("${coordinate.artifact}-${coordinate.version}.jar")
            .normalize()
    require(artifactPath.startsWith(cacheRoot)) {
        "Plugin coordinate '${coordinate.value}' resolves outside plugin cache directory."
    }
    return artifactPath
}
