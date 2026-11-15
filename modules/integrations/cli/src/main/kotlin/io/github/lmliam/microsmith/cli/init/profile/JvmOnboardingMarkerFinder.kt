package io.github.lmliam.microsmith.cli.init.profile

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.visitFileTree

internal object JvmOnboardingMarkerFinder {
    fun find(projectRoot: Path, buildMarkers: List<Path>, sourceRootMatcher: (Path) -> Boolean): List<String> =
        try {
            val matchedBuildMarkers =
                buildMarkers
                    .filter { markerPath -> projectRoot.resolve(markerPath).isRegularFile() }
                    .map(Path::toString)
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

    private fun findRootSourceMarkers(projectRoot: Path, sourceRootMatcher: (Path) -> Boolean): List<String> {
        val srcDirectory = projectRoot.resolve(SOURCE_ROOT_DIRECTORY_NAME)
        if (!srcDirectory.isDirectory()) {
            return emptyList()
        }

        return srcDirectory
            .listDirectoryEntries()
            .filter { sourceSetDirectory -> sourceSetDirectory.isDirectory() }
            .flatMap { sourceSetDirectory ->
                sourceSetDirectory
                    .listDirectoryEntries()
                    .filter { sourceTypeDirectory -> sourceTypeDirectory.isDirectory() }
                    .map(projectRoot::relativize)
                    .filter(sourceRootMatcher)
                    .map(Path::toString)
            }
            .sorted()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun findModuleSourceMarkers(projectRoot: Path, sourceRootMatcher: (Path) -> Boolean): List<String> {
        val matchedMarkers = mutableListOf<String>()
        projectRoot.visitFileTree(maxDepth = MODULE_SOURCE_SEARCH_DEPTH) {
            onPreVisitDirectory { directory, _ ->
                if (directory == projectRoot) {
                    FileVisitResult.CONTINUE
                } else {
                    val relativeDirectory = projectRoot.relativize(directory)
                    if (isIgnoredNestedScanRootDirectory(relativeDirectory)) {
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
            }
            onVisitFileFailed { _, _ -> FileVisitResult.CONTINUE }
            onPostVisitDirectory { _, _ -> FileVisitResult.CONTINUE }
        }
        return matchedMarkers.sorted()
    }

    private fun isRootSourceMarker(relativeDirectory: Path, sourceRootMatcher: (Path) -> Boolean): Boolean =
        relativeDirectory.nameCount == ROOT_SOURCE_MARKER_DEPTH &&
            relativeDirectory.getName(0).toString() == SOURCE_ROOT_DIRECTORY_NAME &&
            sourceRootMatcher(relativeDirectory)

    private fun isIgnoredNestedScanRootDirectory(relativeDirectory: Path): Boolean =
        relativeDirectory.nameCount == INFRASTRUCTURE_DIRECTORY_DEPTH &&
            relativeDirectory.getName(0).toString() in IGNORED_NESTED_SCAN_ROOT_DIRECTORIES
}

private const val MODULE_SOURCE_SEARCH_DEPTH = 6

private const val ROOT_SOURCE_MARKER_DEPTH = 3

private const val INFRASTRUCTURE_DIRECTORY_DEPTH = 1

private const val SOURCE_ROOT_DIRECTORY_NAME = "src"

private val IGNORED_NESTED_SCAN_ROOT_DIRECTORIES =
    setOf(
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
