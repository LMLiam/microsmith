package io.github.lmliam.microsmith.gen.output

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import java.nio.file.FileSystem
import java.nio.file.Path

@Inject
internal class GeneratedOutputUniquenessValidator(private val fileSystem: FileSystem) {
    fun requireUniqueOutputPaths(outputs: List<GeneratedFile>) {
        val duplicates = outputs.groupBy { outputPathKey(it) }.filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details = duplicates.keys.sorted().joinToString(", ")
            "Duplicate output file paths detected: $details"
        }
    }

    private fun outputPathKey(output: GeneratedFile): Path =
        fileSystem.pathOf(output.outputRoot).resolve(fileSystem.pathOf(output.relativePath))
}
