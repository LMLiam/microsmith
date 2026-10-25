package io.github.lmliam.microsmith.runtime.scripting.model

import java.nio.file.Path

object GeneratedOutputRoots {
    @JvmStatic
    fun describe(outputDirectory: Path, roots: List<Path>): String {
        val normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize()
        val normalizedRoots = roots.map { root -> root.toAbsolutePath().normalize() }.distinct().sorted()
        return when (normalizedRoots.size) {
            0 -> normalizedOutputDirectory.toString()
            1 -> normalizedRoots.single().toString()
            else ->
                buildString {
                    append(normalizedOutputDirectory)
                    append(" (roots: ")
                    append(normalizedRoots.joinToString())
                    append(')')
                }
        }
    }
}
