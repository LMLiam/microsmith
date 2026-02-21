package me.liam.microsmith.cli.plugins

import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.transfer.ArtifactNotFoundException
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import java.nio.file.Files
import java.nio.file.Path

internal interface RemotePluginResolver {
    fun resolve(
        coordinate: Coordinate,
        repositories: List<String>,
        cacheDirectory: Path,
        offline: Boolean,
    ): ResolvedRemotePlugin
}

internal data class ResolvedRemotePlugin(val rootArtifactPath: Path, val classpath: List<Path>)

internal enum class PluginResolverErrorCategory(val code: String) {
    OFFLINE_CACHE_MISS("offline-cache-miss"),
    DEPENDENCY_RESOLUTION("dependency-resolution"),
    ROOT_ARTIFACT_MISSING("root-artifact-missing"),
}

internal class PluginResolutionDiagnosticException(
    val category: PluginResolverErrorCategory,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

internal class MavenRemotePluginResolver(
    private val repositorySystem: RepositorySystem = RepositorySystemSupplier().get(),
) : RemotePluginResolver {
    override fun resolve(
        coordinate: Coordinate,
        repositories: List<String>,
        cacheDirectory: Path,
        offline: Boolean,
    ): ResolvedRemotePlugin {
        val localRepositoryRoot = pluginArtifactCacheRoot(cacheDirectory)
        Files.createDirectories(localRepositoryRoot)

        val expectedRootArtifactPath = cachePathFor(localRepositoryRoot, coordinate)
        if (offline && !Files.exists(expectedRootArtifactPath)) {
            throw PluginResolutionDiagnosticException(
                category = PluginResolverErrorCategory.OFFLINE_CACHE_MISS,
                message =
                "Offline mode is enabled and plugin '${coordinate.value}' is not in cache at " +
                    "'$expectedRootArtifactPath'. Run once without --offline to populate the cache.",
            )
        }

        val session =
            buildSession(
                repositorySystem = repositorySystem,
                localRepositoryRoot = localRepositoryRoot,
                offline = offline,
            )

        val remoteRepositories = repositories.map { repository -> toRemoteRepository(repository) }
        val dependencyRequest =
            DependencyRequest(
                CollectRequest(
                    Dependency(DefaultArtifact(coordinate.value), JavaScopes.RUNTIME),
                    remoteRepositories,
                ),
                null,
            )

        val dependencyResult =
            try {
                repositorySystem.resolveDependencies(session, dependencyRequest)
            } catch (error: DependencyResolutionException) {
                throw toDiagnostic(coordinate, localRepositoryRoot, repositories, offline, error)
            }

        val classpath = resolveClasspath(dependencyResult.artifactResults, dependencyResult.root)
        val rootArtifactPath =
            expectedRootArtifactPath.takeIf(Files::exists)
                ?: classpath.firstOrNull()
                ?: throw PluginResolutionDiagnosticException(
                    category = PluginResolverErrorCategory.ROOT_ARTIFACT_MISSING,
                    message =
                    "Plugin '${coordinate.value}' resolved but no root jar was produced in cache at " +
                        "'$expectedRootArtifactPath'.",
                )

        return ResolvedRemotePlugin(rootArtifactPath = rootArtifactPath, classpath = classpath)
    }
}

private fun buildSession(
    repositorySystem: RepositorySystem,
    localRepositoryRoot: Path,
    offline: Boolean,
): RepositorySystemSession {
    val sessionBuilder = DefaultRepositorySystemSession()
    val localRepository = LocalRepository(localRepositoryRoot.toFile())
    sessionBuilder.setOffline(offline)
    sessionBuilder.setLocalRepositoryManager(
        repositorySystem.newLocalRepositoryManager(sessionBuilder, localRepository),
    )
    return sessionBuilder
}

private fun toRemoteRepository(repository: String): RemoteRepository = RemoteRepository
    .Builder(repository, "default", repository)
    .build()

private fun resolveClasspath(
    artifactResults: List<ArtifactResult>,
    root: org.eclipse.aether.graph.DependencyNode?,
): List<Path> {
    val visitor = PreorderNodeListGenerator()
    root?.accept(visitor)
    val visited = visitor.files.map { file -> file.toPath().toAbsolutePath().normalize() }
    val fallback =
        artifactResults
            .mapNotNull { result -> result.artifact?.file?.toPath() }
            .map { path -> path.toAbsolutePath().normalize() }
    return (visited + fallback).distinct()
}

private fun toDiagnostic(
    coordinate: Coordinate,
    localRepositoryRoot: Path,
    repositories: List<String>,
    offline: Boolean,
    error: DependencyResolutionException,
): PluginResolutionDiagnosticException {
    val cause: Throwable? = error.result.collectExceptions.firstOrNull() ?: error.cause
    val repositoryList = repositories.joinToString(", ")
    val reason =
        when (cause) {
            is ArtifactNotFoundException ->
                "Artifact '${cause.artifact}' was not found in configured repositories."

            else -> cause?.message ?: error.message ?: "Unknown resolver failure."
        }

    val remediation =
        if (offline) {
            "Offline mode is enabled. Ensure the full dependency graph is cached under " +
                "'$localRepositoryRoot' by running once without --offline."
        } else {
            "Verify plugin coordinates and repository availability."
        }

    return PluginResolutionDiagnosticException(
        category = PluginResolverErrorCategory.DEPENDENCY_RESOLUTION,
        message =
        "Could not resolve plugin '${coordinate.value}' with transitive dependencies. " +
            "Repositories: $repositoryList. $reason $remediation",
        cause = error,
    )
}
