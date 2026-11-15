package io.github.lmliam.microsmith.plugin.services.dotnet.packages

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackagesPluginScope
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver

@DependencyGraph(DotnetPackagesPluginScope::class)
internal interface DotnetPackagesPluginGraph {
    val services: DotnetPackagesPluginServices

    @Provides fun dotnetWorkspaceResolver(): DotnetWorkspaceResolver = DotnetWorkspaceResolver()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): DotnetPackagesPluginGraph
    }
}
