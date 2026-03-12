package me.liam.microsmith.gen.helpers

import me.liam.microsmith.gen.files.GeneratedFile

internal object GeneratedOutputUniquenessValidator {
    fun requireUniqueRelativePaths(outputs: List<GeneratedFile>) {
        val duplicates =
            outputs
                .groupBy { it.relativePath.normalize().toString() }
                .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details = duplicates.keys.sorted().joinToString(", ")
            "Duplicate output file paths detected: $details"
        }
    }
}
