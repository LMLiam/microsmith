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
        val normalizedRelativePath = requireValidRelativePath(output.relativePath)

        return output.outputRoot.normalize()
            .resolve(normalizedRelativePath)
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

    private fun requireValidRelativePath(relativePath: Path): Path {
        require(!relativePath.isAbsolute) {
            "Generated output path must be relative, but was '$relativePath'."
        }

        val normalizedRelativePath = relativePath.normalize()
        require(!normalizedRelativePath.startsWith(Path.of(".."))) {
            "Generated output path '$relativePath' escapes the run output root."
        }
        return normalizedRelativePath
    }
}
