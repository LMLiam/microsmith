package me.liam.microsmith.cli.plugins

import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.resolution.ArtifactResult

internal data class MavenDependencyGraph(
    val root: DependencyNode?,
    val artifactResults: List<ArtifactResult>,
)
