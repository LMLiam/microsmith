package io.github.lmliam.microsmith.gen.execution

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.ArtifactContributionService
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.compile.ArtifactCompilationService
import io.github.lmliam.microsmith.gen.RunScope
import io.github.lmliam.microsmith.gen.render.ArtifactRenderingService
import io.github.lmliam.microsmith.resolve.ResolvedModel

@Inject
@SingleIn(RunScope::class)
internal class ArtifactGenerationPipeline(
    private val artifactContributionService: ArtifactContributionService,
    private val artifactAssemblyService: ArtifactAssemblyService,
    private val artifactCompilationService: ArtifactCompilationService,
    private val artifactRenderingService: ArtifactRenderingService,
) {
    fun generate(resolvedModels: List<ResolvedModel>): List<GeneratedFile> {
        val contributions = artifactContributionService.contribute(resolvedModels)
        val assembly = artifactAssemblyService.assemble(contributions)
        val compiledAssembly = artifactCompilationService.compile(assembly)

        return artifactRenderingService.render(compiledAssembly)
    }
}
