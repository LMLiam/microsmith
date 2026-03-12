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
            val rootSourceMarkers = findRootSourceMarkers(projectRoot, sourceRootMatcher)

            when {
                rootSourceMarkers.isNotEmpty() -> {
                    (matchedBuildMarkers + rootSourceMarkers).sorted()
                }

                matchedBuildMarkers.isEmpty() -> {
                    emptyList()
                }

                else -> {
                    val matchedModuleSourceMarkers = findModuleSourceMarkers(projectRoot, sourceRootMatcher)
                    if (matchedModuleSourceMarkers.isEmpty()) {
                        emptyList()
                    } else {
                        (matchedBuildMarkers + matchedModuleSourceMarkers).sorted()
                    }
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

    private fun findRootSourceMarkers(projectRoot: Path, sourceRootMatcher: (Path) -> Boolean): List<String> {
        val srcDirectory = projectRoot.resolve(SOURCE_ROOT_DIRECTORY_NAME)
        if (!Files.isDirectory(srcDirectory)) {
            return emptyList()
        }

        return Files.newDirectoryStream(srcDirectory).use { sourceSetDirectories ->
            sourceSetDirectories
                .asSequence()
                .filter(Files::isDirectory)
                .flatMap { sourceSetDirectory ->
                    Files.newDirectoryStream(sourceSetDirectory).use { sourceTypeDirectories ->
                        sourceTypeDirectories
                            .asSequence()
                            .filter(Files::isDirectory)
                            .map(projectRoot::relativize)
                            .filter(sourceRootMatcher)
                            .map(Path::toString)
                            .toList()
                            .asSequence()
                    }
                }
                .sorted()
                .toList()
        }
    }

    private fun findModuleSourceMarkers(projectRoot: Path, sourceRootMatcher: (Path) -> Boolean): List<String> {
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
                    return if (isRootSourceMarker(relativeDirectory, sourceRootMatcher)) {
                        FileVisitResult.SKIP_SUBTREE
                    } else if (sourceRootMatcher(relativeDirectory)) {
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

    private fun isRootSourceMarker(relativeDirectory: Path, sourceRootMatcher: (Path) -> Boolean): Boolean {
        return relativeDirectory.nameCount == ROOT_SOURCE_MARKER_DEPTH &&
            relativeDirectory.getName(0).toString() == SOURCE_ROOT_DIRECTORY_NAME &&
            sourceRootMatcher(relativeDirectory)
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

private const val SOURCE_ROOT_DIRECTORY_NAME = "src"
