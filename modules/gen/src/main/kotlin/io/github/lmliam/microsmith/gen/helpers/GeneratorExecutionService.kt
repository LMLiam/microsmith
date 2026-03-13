package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.core.MicrosmithModel
import io.github.lmliam.microsmith.dsl.helpers.extensions
import io.github.lmliam.microsmith.gen.core.GeneratorRegistry.getGenerator
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
