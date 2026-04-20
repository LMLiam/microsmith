package io.github.lmliam.microsmith.runtime.scripting.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

object GeneratedOutputRootsLocator {
    private val originsDirectoryName = ".microsmith"
    private val originsManifestName = "origins.json"

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
        return when (roots.size) {
            0 -> normalizedOutputDirectory.toString()
            1 -> roots.single().toString()
            else -> buildString {
                append(normalizedOutputDirectory)
                append(" (roots: ")
                append(roots.joinToString())
                append(')')
            }
        }
    }

    private fun isOriginsManifest(path: Path): Boolean =
        path.isRegularFile() &&
            path.name == originsManifestName &&
            path.parent?.name == originsDirectoryName
}
