package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.gen.files.FileSpace

internal object GenerationProgressReporter {
    fun reportMissingGenerator(extension: MicrosmithExtension) {
        println("No generator found for ${extension::class.simpleName}")
    }

    fun reportGeneratedFiles(extension: MicrosmithExtension, fileCount: Int, space: FileSpace) {
        println("Generated $fileCount files for ${extension::class.simpleName} in ${space.root}")
    }

    fun reportModelGenerationComplete(space: FileSpace) {
        println("Generated all files in ${space.root}")
    }
}
