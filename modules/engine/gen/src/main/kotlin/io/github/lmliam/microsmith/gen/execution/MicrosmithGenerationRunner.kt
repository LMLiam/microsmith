package io.github.lmliam.microsmith.gen.execution

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.RunScope
import io.github.lmliam.microsmith.gen.errors.GenerationResolutionFailedException
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.TemporaryDirectories
import io.github.lmliam.microsmith.gen.output.GeneratedOutputUniquenessValidator
import io.github.lmliam.microsmith.gen.output.GeneratedOutputWriter
import io.github.lmliam.microsmith.resolve.DomainResolutionService
import io.github.lmliam.microsmith.resolve.ResolutionOutcome
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnosticService
import java.nio.file.Path

@Inject
@SingleIn(RunScope::class)
internal class MicrosmithGenerationRunner(
    private val domainResolutionService: DomainResolutionService,
    private val resolutionDiagnosticService: ResolutionDiagnosticService,
    private val artifactPipeline: ArtifactGenerationPipeline,
    private val uniquenessValidator: GeneratedOutputUniquenessValidator,
    private val outputWriter: GeneratedOutputWriter,
    private val temporaryDirectories: TemporaryDirectories,
    private val progressReporter: GenerationProgressReporter,
) {
    suspend fun generate(model: MicrosmithModel, finalDir: FileSpace): List<Path> {
        val outputs =
            temporaryDirectories.create().use { tempSpace ->
                val resolvedModels =
                    when (val resolution = domainResolutionService.resolve(model)) {
                        is ResolutionOutcome.Success -> resolution.models

                        is ResolutionOutcome.Failure -> {
                            val diagnostics = resolutionDiagnosticService.describe(resolution.issues)
                            throw GenerationResolutionFailedException(
                                issues = resolution.issues,
                                diagnostics = diagnostics,
                            )
                        }
                    }

                val generatedFiles = artifactPipeline.generate(resolvedModels)

                uniquenessValidator.requireUniqueOutputPaths(generatedFiles)
                outputWriter.write(generatedFiles, tempSpace)

                generatedFiles
            }

        outputWriter.write(outputs, finalDir)

        progressReporter.reportModelGenerationComplete(finalDir)

        return outputWriter.outputRoots(outputs, finalDir)
    }
}
