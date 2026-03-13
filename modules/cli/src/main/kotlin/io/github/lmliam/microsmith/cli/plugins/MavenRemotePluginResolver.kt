package io.github.lmliam.microsmith.cli.plugins

import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.supplier.RepositorySystemSupplier
import java.nio.file.Files
import java.nio.file.Path

internal class MavenRemotePluginResolver(
    private val repositorySystem: RepositorySystem = RepositorySystemSupplier().get(),
    private val sessionFactory: MavenRepositorySessionFactory = MavenRepositorySessionFactory(),
    private val remoteRepositoryFactory: AetherRemoteRepositoryFactory = AetherRemoteRepositoryFactory(),
    private val dependencyGraphResolver: MavenDependencyGraphResolver = MavenDependencyGraphResolver(),
    private val resolvedRemotePluginFactory: ResolvedRemotePluginFactory = ResolvedRemotePluginFactory(),
) : RemotePluginResolver {
    override fun resolve(
        coordinate: Coordinate,
        repositories: List<RepositoryEndpoint>,
        cacheDirectory: Path,
        offline: Boolean,
    ): ResolvedRemotePlugin {
        val localRepositoryRoot =
            pluginArtifactCacheRoot(cacheDirectory).also { root ->
                Files.createDirectories(root)
            }
        val expectedRootArtifactPath = cachePathFor(localRepositoryRoot, coordinate)
        ensureOfflineRootAvailability(
            coordinate = coordinate,
            expectedRootArtifactPath = expectedRootArtifactPath,
            offline = offline,
        )

        val session = sessionFactory.create(repositorySystem, localRepositoryRoot, offline)
        val remoteRepositories = repositories.mapIndexed(remoteRepositoryFactory::create)
        val dependencyGraph =
            dependencyGraphResolver.resolve(
                repositorySystem = repositorySystem,
                session = session,
                request =
                MavenDependencyGraphRequest(
                    coordinate = coordinate,
                    repositories = remoteRepositories,
                    localRepositoryRoot = localRepositoryRoot,
                    offline = offline,
                ),
            )
        return resolvedRemotePluginFactory.create(
            coordinate = coordinate,
            dependencyGraph = dependencyGraph,
            localRepositoryRoot = localRepositoryRoot,
            expectedRootArtifactPath = expectedRootArtifactPath,
        )
    }

    private fun ensureOfflineRootAvailability(
        coordinate: Coordinate,
        expectedRootArtifactPath: Path,
        offline: Boolean,
    ) {
        if (!offline || Files.exists(expectedRootArtifactPath)) {
            return
        }

        throw PluginResolutionDiagnosticException(
            category = PluginResolverErrorCategory.OFFLINE_CACHE_MISS,
            message =
            "Offline mode is enabled and plugin '${coordinate.value}' is not in cache at " +
                "'$expectedRootArtifactPath'. Run once without --offline to populate the cache.",
        )
    }
}
