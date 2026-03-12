package me.liam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile

internal object JvmOnboardingMarkerFinder {
    fun find(projectRoot: Path, sourceRootMatcher: (Path) -> Boolean): List<String> {
        return try {
            val matchedBuildMarkers =
                JVM_BUILD_MARKERS.filter { markerFileName ->
                    projectRoot.resolve(markerFileName).isRegularFile()
                }
            val matchedSourceMarkers = findSourceMarkers(projectRoot, sourceRootMatcher)
            val rootSourceMarkers =
                matchedSourceMarkers.filter(::isRootSourceMarker)

            when {
                rootSourceMarkers.isNotEmpty() -> {
                    (matchedBuildMarkers + rootSourceMarkers).sorted()
                }

                matchedBuildMarkers.isEmpty() -> {
                    emptyList()
                }

                matchedSourceMarkers.isEmpty() -> {
                    emptyList()
                }

                else -> {
                    (matchedBuildMarkers + matchedSourceMarkers).sorted()
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

    private fun findSourceMarkers(projectRoot: Path, sourceRootMatcher: (Path) -> Boolean): List<String> {
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
                    return if (sourceRootMatcher(relativeDirectory)) {
                        matchedMarkers += relativeDirectory.toString()
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        FileVisitResult.CONTINUE
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

    private fun isRootSourceMarker(markerPath: String): Boolean {
        return Path.of(markerPath).nameCount == ROOT_SOURCE_MARKER_DEPTH
    }
}

private val JVM_BUILD_MARKERS = listOf(
    "pom.xml",
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
)

private const val MODULE_SOURCE_SEARCH_DEPTH = 6

private const val ROOT_SOURCE_MARKER_DEPTH = 3
