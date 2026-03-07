package me.liam.microsmith.cli.plugins

import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator
import java.nio.file.Files
import java.nio.file.Path

/** Normalizes Aether results into cache-relative lock keys and a deterministic runtime classpath. */
internal class ResolvedRemotePluginFactory {
    fun create(
        coordinate: Coordinate,
        dependencyGraph: MavenDependencyGraph,
        localRepositoryRoot: Path,
        expectedRootArtifactPath: Path,
    ): ResolvedRemotePlugin {
        val classpath = resolveClasspath(dependencyGraph.artifactResults, dependencyGraph.root)
        val rootArtifactPath =
            resolveRootArtifactPath(
                coordinate = coordinate,
                expectedRootArtifactPath = expectedRootArtifactPath,
                artifactResults = dependencyGraph.artifactResults,
            )
        val artifacts =
            resolveArtifacts(
                coordinate = coordinate,
                artifactResults = dependencyGraph.artifactResults,
                localRepositoryRoot = localRepositoryRoot,
                rootArtifactPath = rootArtifactPath,
            )

        return ResolvedRemotePlugin(
            rootArtifactPath = rootArtifactPath,
            classpath = classpath,
            artifacts = artifacts,
        )
    }

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

    private fun resolveRootArtifactPath(
        coordinate: Coordinate,
        expectedRootArtifactPath: Path,
        artifactResults: List<ArtifactResult>,
    ): Path = expectedRootArtifactPath.takeIf(Files::exists)
        ?: artifactResults
            .asSequence()
            .mapNotNull { result -> result.artifact }
            .firstOrNull { artifact -> artifact.matchesCoordinate(coordinate) }
            ?.file
            ?.toPath()
            ?.toAbsolutePath()
            ?.normalize()
        ?: throw PluginResolutionDiagnosticException(
            category = PluginResolverErrorCategory.ROOT_ARTIFACT_MISSING,
            message =
            "Plugin '${coordinate.value}' resolved but no root jar was produced in cache at " +
                "'$expectedRootArtifactPath'.",
        )

    private fun resolveArtifacts(
        coordinate: Coordinate,
        artifactResults: List<ArtifactResult>,
        localRepositoryRoot: Path,
        rootArtifactPath: Path,
    ): List<ResolvedRemoteArtifact> {
        val resolvedRuntimeArtifacts =
            artifactResults
                .asSequence()
                .mapNotNull { result -> result.artifact?.file?.toPath() }
                .map { path -> path.toAbsolutePath().normalize() }
                .filter(Files::exists)
                .toList()
        val resolvedDescriptorArtifacts =
            artifactResults
                .asSequence()
                .mapNotNull { result -> result.artifact }
                .map { artifact ->
                    resolveDescriptorPath(
                        coordinate = coordinate,
                        localRepositoryRoot = localRepositoryRoot,
                        artifact = artifact,
                    )
                }.toList()

        return (resolvedRuntimeArtifacts + listOf(rootArtifactPath) + resolvedDescriptorArtifacts)
            .distinct()
            .map { artifactPath ->
                ResolvedRemoteArtifact(
                    lockKey =
                    toRemoteArtifactLockKey(
                        coordinate = coordinate,
                        localRepositoryRoot = localRepositoryRoot,
                        artifactPath = artifactPath,
                    ),
                    artifactPath = artifactPath,
                )
            }.sortedBy(ResolvedRemoteArtifact::lockKey)
    }

    private fun resolveDescriptorPath(coordinate: Coordinate, localRepositoryRoot: Path, artifact: Artifact): Path {
        val fromArtifactFile =
            artifact.file
                ?.toPath()
                ?.toAbsolutePath()
                ?.normalize()
                ?.let(::toPomSiblingPath)
        val fromArtifactCoordinates =
            localRepositoryRoot
                .resolve(artifact.groupId.replace('.', '/'))
                .resolve(artifact.artifactId)
                .resolve(artifact.version)
                .resolve("${artifact.artifactId}-${artifact.version}.pom")
                .toAbsolutePath()
                .normalize()
        val candidates = listOfNotNull(fromArtifactFile, fromArtifactCoordinates).distinct()
        val resolvedPath = candidates.firstOrNull(Files::exists) ?: candidates.first()
        require(Files.exists(resolvedPath) && Files.isRegularFile(resolvedPath)) {
            "Dependency descriptor for '${artifact.groupId}:${artifact.artifactId}:${artifact.version}' " +
                "is missing from cache at '$resolvedPath'."
        }
        require(resolvedPath.startsWith(localRepositoryRoot)) {
            "Resolved descriptor for plugin '${coordinate.value}' escapes plugin cache root: '$resolvedPath'."
        }
        return resolvedPath
    }

    private fun toPomSiblingPath(artifactPath: Path): Path? {
        val fileName = artifactPath.fileName?.toString() ?: return null
        val extensionSeparator = fileName.lastIndexOf('.')
        if (extensionSeparator <= 0) {
            return null
        }
        val pomName = fileName.substring(0, extensionSeparator) + ".pom"
        return artifactPath.resolveSibling(pomName).toAbsolutePath().normalize()
    }

    private fun toRemoteArtifactLockKey(
        coordinate: Coordinate,
        localRepositoryRoot: Path,
        artifactPath: Path,
    ): String {
        val normalizedArtifactPath = artifactPath.toAbsolutePath().normalize()
        require(normalizedArtifactPath.startsWith(localRepositoryRoot)) {
            "Resolved artifact for plugin '${coordinate.value}' escapes plugin cache root: '$artifactPath'."
        }
        return localRepositoryRoot.relativize(normalizedArtifactPath).toString().replace('\\', '/')
    }

    private fun Artifact.matchesCoordinate(coordinate: Coordinate): Boolean =
        groupId == coordinate.group && artifactId == coordinate.artifact && version == coordinate.version
}
