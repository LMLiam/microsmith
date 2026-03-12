package me.liam.microsmith.gen.helpers

import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.gen.core.GeneratorRegistry
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.TemporaryDirectory

internal class MicrosmithGenerationRunner(
    private val generatorRegistryLoader: () -> Unit = GeneratorRegistry::load,
    private val generatorExecutionService: GeneratorExecutionService = GeneratorExecutionService(),
    private val outputUniquenessValidator: GeneratedOutputUniquenessValidator = GeneratedOutputUniquenessValidator,
    private val outputWriter: GeneratedOutputWriter = GeneratedOutputWriter(),
    private val progressReporter: GenerationProgressReporter = GenerationProgressReporter,
) {
    suspend fun generate(model: MicrosmithModel, finalDir: FileSpace) {
        generatorRegistryLoader()

        val outputs =
            TemporaryDirectory.create().use { tempSpace ->
                // Generators still receive the final destination so any output-root-aware generator
                // continues to see the stable repository path while staging validates write safety.
                val generated = generatorExecutionService.generate(model, finalDir)
                outputUniquenessValidator.requireUniqueRelativePaths(generated)
                outputWriter.write(generated, tempSpace)
                generated
            }

        outputWriter.write(outputs, finalDir)
        progressReporter.reportModelGenerationComplete(finalDir)
    }
}
