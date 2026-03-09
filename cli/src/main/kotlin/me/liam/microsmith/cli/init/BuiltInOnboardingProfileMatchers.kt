package me.liam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile

internal object BuiltInOnboardingProfileMatchers {
    fun all(dotnetMarkerFinder: (Path) -> String? = ::findDotnetMarker): List<OnboardingProfileMatcher> {
        return listOf(
            rootMarkerMatcher(NodeOnboardingProfile, "package.json"),
            rootMarkerMatcher(GoOnboardingProfile, "go.mod"),
            OnboardingProfileMatcher(DotnetOnboardingProfile) { projectRoot ->
                safeFindDotnetMarker(projectRoot, dotnetMarkerFinder)?.let(::listOf).orEmpty()
            },
        )
    }

    private fun rootMarkerMatcher(profile: OnboardingProfile, markerFileName: String): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(profile) { projectRoot ->
            if (projectRoot.resolve(markerFileName).isRegularFile()) {
                listOf(markerFileName)
            } else {
                emptyList()
            }
        }
    }
}

private fun safeFindDotnetMarker(projectRoot: Path, dotnetMarkerFinder: (Path) -> String?): String? = try {
    dotnetMarkerFinder(projectRoot)
} catch (_: IOException) {
    null
} catch (_: UncheckedIOException) {
    null
} catch (_: SecurityException) {
    null
}

private fun findDotnetMarker(projectRoot: Path): String? {
    val matchedMarkers = mutableListOf<String>()
    Files.walkFileTree(
        projectRoot,
        emptySet(),
        DOTNET_MARKER_SEARCH_DEPTH,
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

            override fun visitFileFailed(file: Path, exception: IOException): FileVisitResult = FileVisitResult.CONTINUE

            override fun postVisitDirectory(directory: Path, exception: IOException?): FileVisitResult =
                FileVisitResult.CONTINUE
        },
    )
    return matchedMarkers.minOrNull()
}

private const val DOTNET_MARKER_SEARCH_DEPTH = 6
