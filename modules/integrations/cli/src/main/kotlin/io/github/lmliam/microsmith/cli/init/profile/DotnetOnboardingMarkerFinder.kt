package io.github.lmliam.microsmith.cli.init.profile

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.visitFileTree

internal object DotnetOnboardingMarkerFinder {
    fun find(projectRoot: Path): String? =
        try {
            findMatchingProjectFile(projectRoot)
        } catch (_: IOException) {
            null
        } catch (_: UncheckedIOException) {
            null
        } catch (_: SecurityException) {
            null
        }

    @OptIn(ExperimentalPathApi::class)
    private fun findMatchingProjectFile(projectRoot: Path): String? {
        val matchedMarkers = mutableListOf<String>()
        projectRoot.visitFileTree(maxDepth = SEARCH_DEPTH) {
            onVisitFile { file, attributes ->
                if (attributes.isRegularFile) {
                    val relativePath = projectRoot.relativize(file).toString()
                    if (relativePath.endsWith(".sln") || relativePath.endsWith(".csproj")) {
                        matchedMarkers += relativePath
                    }
                }
                FileVisitResult.CONTINUE
            }
            onVisitFileFailed { _, _ -> FileVisitResult.CONTINUE }
            onPostVisitDirectory { _, _ -> FileVisitResult.CONTINUE }
        }
        return matchedMarkers.minOrNull()
    }
}

private const val SEARCH_DEPTH = 6
