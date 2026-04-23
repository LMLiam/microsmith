package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.artifact.core.ArtifactAssemblyService
import io.github.lmliam.microsmith.artifact.core.ArtifactContributionService
import io.github.lmliam.microsmith.compile.core.ArtifactCompilationService
import io.github.lmliam.microsmith.dsl.core.MicrosmithModel
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.TemporaryDirectory
import io.github.lmliam.microsmith.resolve.core.DomainResolutionService
import java.nio.file.Path

internal class MicrosmithGenerationRunner(
    private val domainResolutionService: DomainResolutionService = DomainResolutionService(),
    private val artifactContributionService: ArtifactContributionService = ArtifactContributionService(),
    private val artifactAssemblyService: ArtifactAssemblyService = ArtifactAssemblyService(),
    private val artifactCompilationService: ArtifactCompilationService = ArtifactCompilationService(),
    private val artifactRenderingService: ArtifactRenderingService = ArtifactRenderingService(),
    private val outputUniquenessValidator: GeneratedOutputUniquenessValidator = GeneratedOutputUniquenessValidator,
    private val outputWriter: GeneratedOutputWriter = GeneratedOutputWriter(),
    private val progressReporter: GenerationProgressReporter = GenerationProgressReporter,
) {
    suspend fun generate(model: MicrosmithModel, finalDir: FileSpace): List<Path> {
        val outputs =
            TemporaryDirectory.create().use { tempSpace ->
                val resolvedModels = domainResolutionService.resolve(model)
                val contributions = artifactContributionService.contribute(resolvedModels)
                val assembly = artifactAssemblyService.assemble(contributions)
                val compiledAssembly = artifactCompilationService.compile(assembly)
                val generatedWithOriginsManifest =
                    GeneratedOriginsManifestBuilder.appendTo(artifactRenderingService.render(compiledAssembly))
                outputUniquenessValidator.requireUniqueOutputPaths(generatedWithOriginsManifest)
                outputWriter.write(generatedWithOriginsManifest, tempSpace)
                generatedWithOriginsManifest
            }

        outputWriter.write(outputs, finalDir)
        progressReporter.reportModelGenerationComplete(finalDir)
        return outputs
            .map { generatedFile -> finalDir.root.resolve(generatedFile.outputRoot).normalize() }
            .distinct()
            .sorted()
    }
}
