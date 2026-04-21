package io.github.lmliam.microsmith.runtime.scripting.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

object GeneratedOutputRootsLocator {
    private const val ORIGINS_DIRECTORY_NAME = ".microsmith"
    private const val ORIGINS_MANIFEST_NAME = "origins.json"

    @JvmStatic
    fun locate(outputDirectory: Path): List<Path> {
        val normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize()
        if (!normalizedOutputDirectory.exists()) {
            return emptyList()
        }

        Files.walk(normalizedOutputDirectory).use { paths ->
            return paths
                .filter(::isOriginsManifest)
                .map { manifestPath ->
                    manifestPath.parent?.parent ?: normalizedOutputDirectory
                }.map(Path::normalize)
                .distinct()
                .sorted()
                .toList()
        }
    }

    @JvmStatic
    fun describe(outputDirectory: Path): String {
        val normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize()
        val roots = locate(normalizedOutputDirectory)
        return describe(normalizedOutputDirectory, roots)
    }

    @JvmStatic
    fun describe(outputDirectory: Path, roots: List<Path>): String {
        val normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize()
        val normalizedRoots = roots.map { root -> root.toAbsolutePath().normalize() }.distinct().sorted()
        return when (normalizedRoots.size) {
            0 -> normalizedOutputDirectory.toString()
            1 -> normalizedRoots.single().toString()
            else -> buildString {
                append(normalizedOutputDirectory)
                append(" (roots: ")
                append(normalizedRoots.joinToString())
                append(')')
            }
        }
    }

    private fun isOriginsManifest(path: Path): Boolean = path.isRegularFile() &&
        path.name == ORIGINS_MANIFEST_NAME &&
        path.parent?.name == ORIGINS_DIRECTORY_NAME
}
