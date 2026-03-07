package me.liam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isRegularFile

internal enum class OnboardingRepositoryType(
    val displayName: String,
    val sampleMessageName: String,
    val repoNativeOutputDirectory: String?,
) {
    NODE(
        displayName = "Node",
        sampleMessageName = "NodeUserCreated",
        repoNativeOutputDirectory = "./generated",
    ),
    GO(
        displayName = "Go",
        sampleMessageName = "GoUserCreated",
        repoNativeOutputDirectory = "./internal/gen",
    ),
    DOTNET(
        displayName = ".NET",
        sampleMessageName = "DotnetUserCreated",
        repoNativeOutputDirectory = "./Generated",
    ),
    OTHER(
        displayName = "Other",
        sampleMessageName = "UserCreated",
        repoNativeOutputDirectory = null,
    ),
}

internal fun detectOnboardingRepositoryType(
    projectRoot: Path,
    dotnetMarkerFinder: (Path) -> String? = ::findDotnetMarker,
): OnboardingRepositoryDetection {
    val matchedDetections = buildList {
        if (projectRoot.resolve("package.json").isRegularFile()) {
            add(OnboardingRepositoryType.NODE to "package.json")
        }
        if (projectRoot.resolve("go.mod").isRegularFile()) {
            add(OnboardingRepositoryType.GO to "go.mod")
        }
        safeFindDotnetMarker(projectRoot, dotnetMarkerFinder)?.let { marker ->
            add(OnboardingRepositoryType.DOTNET to marker)
        }
    }

    val distinctTypes = matchedDetections.map { (type, _) -> type }.distinct()
    val resolvedType =
        when (distinctTypes.size) {
            0 -> OnboardingRepositoryType.OTHER
            1 -> distinctTypes.single()
            else -> OnboardingRepositoryType.OTHER
        }

    return OnboardingRepositoryDetection(
        type = resolvedType,
        matchedMarkers = matchedDetections.map { (_, marker) -> marker }.sorted(),
    )
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
    return matchedMarkers.sorted().firstOrNull()
}

private const val DOTNET_MARKER_SEARCH_DEPTH = 6
