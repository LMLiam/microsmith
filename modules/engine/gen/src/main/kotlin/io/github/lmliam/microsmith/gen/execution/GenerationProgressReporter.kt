package io.github.lmliam.microsmith.gen.execution

import io.github.lmliam.microsmith.gen.files.FileSpace

internal object GenerationProgressReporter {
    fun reportModelGenerationComplete(space: FileSpace) {
        println("Generated all files in ${space.root}")
    }
}
