package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.GeneratedFile
import java.nio.file.Path

internal object GeneratedOutputUniquenessValidator {
    fun requireUniqueOutputPaths(outputs: List<GeneratedFile>) {
        val duplicates =
            outputs
                .groupBy { outputPathKey(it) }
                .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details = duplicates.keys.sorted().joinToString(", ")
            "Duplicate output file paths detected: $details"
        }
    }

    private fun outputPathKey(output: GeneratedFile): String {
        requireValidOutputRoot(output.outputRoot)

        return output.outputRoot.normalize()
            .resolve(output.relativePath.normalize())
            .normalize()
            .toString()
    }

    private fun requireValidOutputRoot(outputRoot: Path) {
        require(!outputRoot.isAbsolute) {
            "Generated output root must be relative, but was '$outputRoot'."
        }

        val normalizedOutputRoot = outputRoot.normalize()
        require(!normalizedOutputRoot.startsWith(Path.of(".."))) {
            "Generated output root '$outputRoot' escapes the run output root."
        }
    }
}
