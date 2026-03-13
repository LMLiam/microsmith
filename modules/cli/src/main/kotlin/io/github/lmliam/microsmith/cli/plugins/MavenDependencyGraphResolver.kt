package io.github.lmliam.microsmith.cli.plugins

import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.resolution.DependencyResolutionException
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.filter.DependencyFilterUtils

internal class MavenDependencyGraphResolver(
    private val diagnostics: PluginDependencyResolutionDiagnostics = PluginDependencyResolutionDiagnostics(),
) {
    fun resolve(
        repositorySystem: RepositorySystem,
        session: RepositorySystemSession,
        request: MavenDependencyGraphRequest,
    ): MavenDependencyGraph {
        val runtimeClasspathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME)
        val dependencyRequest =
            DependencyRequest(
                CollectRequest(
                    Dependency(DefaultArtifact(request.coordinate.value), JavaScopes.RUNTIME),
                    request.repositories,
                ),
                runtimeClasspathFilter,
            )

        return try {
            val dependencyResult = repositorySystem.resolveDependencies(session, dependencyRequest)
            MavenDependencyGraph(
                root = dependencyResult.root,
                artifactResults = dependencyResult.artifactResults,
            )
        } catch (error: DependencyResolutionException) {
            throw diagnostics.toDiagnostic(
                coordinate = request.coordinate,
                localRepositoryRoot = request.localRepositoryRoot,
                repositories = request.repositories.map(RemoteRepository::getUrl),
                offline = request.offline,
                error = error,
            )
        }
    }
}
