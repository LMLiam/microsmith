package io.github.lmliam.microsmith.gen.composition

import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.ArtifactContributionService
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import io.github.lmliam.microsmith.artifact.files.BinaryFileArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactAssembler
import io.github.lmliam.microsmith.compile.ArtifactCompilationService
import io.github.lmliam.microsmith.gen.ArtifactRendererRegistry
import io.github.lmliam.microsmith.gen.RunScope
import io.github.lmliam.microsmith.gen.execution.MicrosmithGenerationRunner
import io.github.lmliam.microsmith.gen.files.render.BinaryFileArtifactRenderer
import io.github.lmliam.microsmith.gen.files.render.TextFileArtifactRenderer
import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.gen.plugins.discoverMicrosmithPlugins
import io.github.lmliam.microsmith.resolve.DomainResolutionService
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnosticService

@GraphExtension(RunScope::class)
internal interface RunGraph {
    val runner: MicrosmithGenerationRunner

    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides pluginClassLoader: ClassLoader): RunGraph
    }

    @Provides
    @SingleIn(RunScope::class)
    fun providePluginCatalog(
        pluginClassLoader: ClassLoader,
        textRenderer: TextFileArtifactRenderer,
        binaryRenderer: BinaryFileArtifactRenderer,
    ): MicrosmithPluginCatalog =
        discoverMicrosmithPlugins(pluginClassLoader)
            .including(
                artifactAssemblers = listOf(TextFileArtifactAssembler(), BinaryFileArtifactAssembler()),
                artifactRenderers = listOf(textRenderer, binaryRenderer),
            )

    @Provides
    @SingleIn(RunScope::class)
    fun provideDomainResolutionService(pluginCatalog: MicrosmithPluginCatalog): DomainResolutionService =
        DomainResolutionService(pluginCatalog.domainResolvers)

    @Provides
    @SingleIn(RunScope::class)
    fun provideResolutionDiagnosticService(pluginCatalog: MicrosmithPluginCatalog): ResolutionDiagnosticService =
        ResolutionDiagnosticService(pluginCatalog.resolutionIssueDiagnosticMappers)

    @Provides
    @SingleIn(RunScope::class)
    fun provideArtifactContributionService(pluginCatalog: MicrosmithPluginCatalog): ArtifactContributionService =
        ArtifactContributionService(pluginCatalog.artifactContributors)

    @Provides
    @SingleIn(RunScope::class)
    fun provideArtifactAssemblyService(pluginCatalog: MicrosmithPluginCatalog): ArtifactAssemblyService =
        ArtifactAssemblyService(pluginCatalog.artifactAssemblers)

    @Provides
    @SingleIn(RunScope::class)
    fun provideArtifactCompilationService(
        pluginCatalog: MicrosmithPluginCatalog,
        assemblyService: ArtifactAssemblyService,
    ): ArtifactCompilationService = ArtifactCompilationService(pluginCatalog.artifactCompilers, assemblyService)

    @Provides
    @SingleIn(RunScope::class)
    fun provideArtifactRendererRegistry(pluginCatalog: MicrosmithPluginCatalog): ArtifactRendererRegistry =
        ArtifactRendererRegistry(pluginCatalog.artifactRenderers)
}
