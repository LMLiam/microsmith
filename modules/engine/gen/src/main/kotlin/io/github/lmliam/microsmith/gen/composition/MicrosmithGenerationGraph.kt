package io.github.lmliam.microsmith.gen.composition

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.ArtifactContributionService
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import io.github.lmliam.microsmith.compile.ArtifactCompilationService
import io.github.lmliam.microsmith.gen.ArtifactRendererRegistry
import io.github.lmliam.microsmith.gen.MicrosmithGenerationScope
import io.github.lmliam.microsmith.gen.execution.MicrosmithGenerationRunner
import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.resolve.DomainResolutionService
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnosticService

@DependencyGraph(scope = MicrosmithGenerationScope::class)
internal interface MicrosmithGenerationGraph {
    val runner: MicrosmithGenerationRunner

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides pluginCatalog: MicrosmithPluginCatalog): MicrosmithGenerationGraph
    }

    @Provides
    @SingleIn(MicrosmithGenerationScope::class)
    fun provideDomainResolutionService(pluginCatalog: MicrosmithPluginCatalog): DomainResolutionService =
        DomainResolutionService(pluginCatalog.domainResolvers)

    @Provides
    @SingleIn(MicrosmithGenerationScope::class)
    fun provideResolutionDiagnosticService(pluginCatalog: MicrosmithPluginCatalog): ResolutionDiagnosticService =
        ResolutionDiagnosticService(pluginCatalog.resolutionIssueDiagnosticMappers)

    @Provides
    @SingleIn(MicrosmithGenerationScope::class)
    fun provideArtifactContributionService(pluginCatalog: MicrosmithPluginCatalog): ArtifactContributionService =
        ArtifactContributionService(pluginCatalog.artifactContributors)

    @Provides
    @SingleIn(MicrosmithGenerationScope::class)
    fun provideArtifactAssemblyService(pluginCatalog: MicrosmithPluginCatalog): ArtifactAssemblyService =
        ArtifactAssemblyService(pluginCatalog.artifactAssemblers)

    @Provides
    @SingleIn(MicrosmithGenerationScope::class)
    fun provideArtifactCompilationService(
        pluginCatalog: MicrosmithPluginCatalog,
        assemblyService: ArtifactAssemblyService,
    ): ArtifactCompilationService = ArtifactCompilationService(pluginCatalog.artifactCompilers, assemblyService)

    @Provides
    @SingleIn(MicrosmithGenerationScope::class)
    fun provideArtifactRendererRegistry(pluginCatalog: MicrosmithPluginCatalog): ArtifactRendererRegistry =
        ArtifactRendererRegistry(pluginCatalog.artifactRenderers)
}
