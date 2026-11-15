package io.github.lmliam.microsmith.plugin.services.dotnet

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetPluginScope
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver

@DependencyGraph(DotnetPluginScope::class)
internal interface DotnetPluginGraph {
    val services: DotnetPluginServices

    @Provides fun dotnetWorkspaceResolver(): DotnetWorkspaceResolver = DotnetWorkspaceResolver()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): DotnetPluginGraph
    }
}
