package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.gen.core.GeneratorRegistry.getGenerator
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile

internal class GeneratorExecutionService(
    private val progressReporter: GenerationProgressReporter = GenerationProgressReporter,
) {
    suspend fun generate(model: MicrosmithModel, space: FileSpace): List<GeneratedFile> = coroutineScope {
        model.extensions()
            .map { extension ->
                async {
                    generate(extension, space)
                }
            }.awaitAll()
            .flatten()
    }

    private suspend fun generate(extension: MicrosmithExtension, space: FileSpace): List<GeneratedFile> {
        val generator = extension.getGenerator()
        if (generator == null) {
            progressReporter.reportMissingGenerator(extension)
            return emptyList()
        }

        return generator.run { extension.generate(space) }.also { generatedFiles ->
            progressReporter.reportGeneratedFiles(extension, generatedFiles.size, space)
        }
    }
}
