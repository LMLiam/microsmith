package io.github.lmliam.microsmith.gen.execution
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.ArtifactContributionService
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import io.github.lmliam.microsmith.compile.ArtifactCompilationService
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.MicrosmithGenerationScope
import io.github.lmliam.microsmith.gen.errors.GenerationResolutionFailedException
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.TemporaryDirectory
import io.github.lmliam.microsmith.gen.output.GeneratedOriginsManifestBuilder
import io.github.lmliam.microsmith.gen.output.GeneratedOutputUniquenessValidator
import io.github.lmliam.microsmith.gen.output.GeneratedOutputWriter
import io.github.lmliam.microsmith.gen.render.ArtifactRenderingService
import io.github.lmliam.microsmith.resolve.DomainResolutionService
import io.github.lmliam.microsmith.resolve.ResolutionOutcome
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnosticService
import java.nio.file.Path

@Inject
@SingleIn(MicrosmithGenerationScope::class)
internal class MicrosmithGenerationRunner(
    private val domainResolutionService: DomainResolutionService,
    private val resolutionDiagnosticService: ResolutionDiagnosticService,
    private val artifactContributionService: ArtifactContributionService,
    private val artifactAssemblyService: ArtifactAssemblyService,
    private val artifactCompilationService: ArtifactCompilationService,
    private val artifactRenderingService: ArtifactRenderingService,
    private val outputWriter: GeneratedOutputWriter,
) {
    suspend fun generate(model: MicrosmithModel, finalDir: FileSpace): List<Path> {
        val outputs = TemporaryDirectory.create().use { tempSpace ->
            val resolvedModels = when (val resolution = domainResolutionService.resolve(model)) {
                is ResolutionOutcome.Success -> resolution.models

                is ResolutionOutcome.Failure -> {
                    val diagnostics = resolutionDiagnosticService.describe(resolution.issues)
                    throw GenerationResolutionFailedException(issues = resolution.issues, diagnostics = diagnostics)
                }
            }

            val contributions = artifactContributionService.contribute(resolvedModels)
            val assembly = artifactAssemblyService.assemble(contributions)
            val compiledAssembly = artifactCompilationService.compile(assembly)
            val generatedWithOriginsManifest = GeneratedOriginsManifestBuilder.appendTo(
                artifactRenderingService.render(compiledAssembly),
            )

            GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(generatedWithOriginsManifest)
            outputWriter.write(generatedWithOriginsManifest, tempSpace)

            generatedWithOriginsManifest
        }

        outputWriter.write(outputs, finalDir)

        GenerationProgressReporter.reportModelGenerationComplete(finalDir)

        return outputs
            .map { generatedFile ->
                finalDir.root
                    .resolve(generatedFile.outputRoot)
                    .normalize()
            }
            .distinct()
            .sorted()
    }
}
