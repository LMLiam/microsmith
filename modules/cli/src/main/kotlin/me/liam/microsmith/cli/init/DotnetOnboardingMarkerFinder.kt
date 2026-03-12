package me.liam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal object DotnetOnboardingMarkerFinder {
    fun find(projectRoot: Path): String? = try {
        findMatchingProjectFile(projectRoot)
    } catch (_: IOException) {
        null
    } catch (_: UncheckedIOException) {
        null
    } catch (_: SecurityException) {
        null
    }

    private fun findMatchingProjectFile(projectRoot: Path): String? {
        val matchedMarkers = mutableListOf<String>()
        Files.walkFileTree(
            projectRoot,
            emptySet(),
            SEARCH_DEPTH,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                    if (attributes.isRegularFile) {
                        val relativePath = projectRoot.relativize(file).toString()
                        if (relativePath.endsWith(".sln") || relativePath.endsWith(".csproj")) {
                            matchedMarkers += relativePath
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exception: IOException): FileVisitResult =
                    FileVisitResult.CONTINUE

                override fun postVisitDirectory(directory: Path, exception: IOException?): FileVisitResult =
                    FileVisitResult.CONTINUE
            },
        )
        return matchedMarkers.minOrNull()
    }
}

private const val SEARCH_DEPTH = 6
