package io.github.lmliam.microsmith.plugin.services.dotnet.asp

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpRenderer
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspPluginScope
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.DotnetAspNamingPolicy
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver

@DependencyGraph(DotnetAspPluginScope::class)
internal interface DotnetAspPluginGraph {
    val services: DotnetAspPluginServices

    @Provides fun csharpRenderer(): CSharpRenderer = CSharpRenderer()

    @Provides fun dotnetAspNamingPolicy(): DotnetAspNamingPolicy = DotnetAspNamingPolicy()

    @Provides fun dotnetWorkspaceResolver(): DotnetWorkspaceResolver = DotnetWorkspaceResolver()

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): DotnetAspPluginGraph
    }
}
