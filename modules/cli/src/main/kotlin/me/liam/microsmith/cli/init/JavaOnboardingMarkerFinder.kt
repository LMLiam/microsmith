package me.liam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

internal object JavaOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> {
        return try {
            val matchedSourceMarkers =
                ROOT_SOURCE_MARKERS.filter { markerPath ->
                    projectRoot.resolve(markerPath).isDirectory()
                }
            val matchedBuildMarkers =
                BUILD_MARKERS.filter { markerFileName ->
                    projectRoot.resolve(markerFileName).isRegularFile()
                }

            if (matchedSourceMarkers.isNotEmpty()) {
                (matchedBuildMarkers + matchedSourceMarkers).sorted()
            } else if (matchedBuildMarkers.isEmpty()) {
                emptyList()
            } else {
                val matchedModuleSourceMarkers = findModuleSourceMarkers(projectRoot)
                if (matchedModuleSourceMarkers.isEmpty()) {
                    emptyList()
                } else {
                    (matchedBuildMarkers + matchedModuleSourceMarkers).sorted()
                }
            }
        } catch (_: IOException) {
            emptyList()
        } catch (_: UncheckedIOException) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun findModuleSourceMarkers(projectRoot: Path): List<String> {
        val matchedMarkers = mutableListOf<String>()
        Files.walkFileTree(
            projectRoot,
            emptySet(),
            MODULE_SOURCE_SEARCH_DEPTH,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(directory: Path, attributes: BasicFileAttributes): FileVisitResult {
                    if (directory == projectRoot) {
                        return FileVisitResult.CONTINUE
                    }

                    val relativeDirectory = projectRoot.relativize(directory)
                    return when {
                        relativeDirectory in ROOT_SOURCE_MARKER_PATHS -> FileVisitResult.SKIP_SUBTREE
                        ROOT_SOURCE_MARKER_PATHS.any(relativeDirectory::endsWith) -> {
                            matchedMarkers += relativeDirectory.toString()
                            FileVisitResult.SKIP_SUBTREE
                        }
                        else -> FileVisitResult.CONTINUE
                    }
                }

                override fun visitFileFailed(file: Path, exception: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE

                override fun postVisitDirectory(directory: Path, exception: IOException?): FileVisitResult =
                    FileVisitResult.CONTINUE
            },
        )
        return matchedMarkers.sorted()
    }
}

private val BUILD_MARKERS = listOf(
    "pom.xml",
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
)

private val ROOT_SOURCE_MARKERS = listOf(
    "src/main/java",
    "src/test/java",
)

private val ROOT_SOURCE_MARKER_PATHS = ROOT_SOURCE_MARKERS.map(Path::of)

private const val MODULE_SOURCE_SEARCH_DEPTH = 6
