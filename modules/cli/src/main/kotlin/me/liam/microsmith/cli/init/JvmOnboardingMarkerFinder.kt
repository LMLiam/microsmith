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
    fun find(projectRoot: Path, buildMarkers: List<Path>, sourceRootMatcher: (Path) -> Boolean): List<String> {
        return try {
            val matchedBuildMarkers =
                buildMarkers
                    .filter { markerPath ->
                        projectRoot.resolve(markerPath).isRegularFile()
                    }.map(Path::toString)
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
                    return if (isIgnoredNestedScanRootDirectory(relativeDirectory)) {
                        FileVisitResult.SKIP_SUBTREE
                    } else if (isRootSourceMarker(relativeDirectory, sourceRootMatcher)) {
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

    private fun isIgnoredNestedScanRootDirectory(relativeDirectory: Path): Boolean {
        return relativeDirectory.nameCount == INFRASTRUCTURE_DIRECTORY_DEPTH &&
            relativeDirectory.getName(0).toString() in IGNORED_NESTED_SCAN_ROOT_DIRECTORIES
    }
}

private const val MODULE_SOURCE_SEARCH_DEPTH = 6

private const val ROOT_SOURCE_MARKER_DEPTH = 3

private const val INFRASTRUCTURE_DIRECTORY_DEPTH = 1

private const val SOURCE_ROOT_DIRECTORY_NAME = "src"

private val IGNORED_NESTED_SCAN_ROOT_DIRECTORIES = setOf(
    ".gradle",
    ".microsmith",
    "build",
    "build-logic",
    "buildSrc",
    "doc",
    "docs",
    "example",
    "examples",
    "gradle",
    "sample",
    "samples",
)
