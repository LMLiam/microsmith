package me.liam.microsmith.cli.init

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

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

internal data class OnboardingRepositoryDetection(
    val type: OnboardingRepositoryType,
    val matchedMarkers: List<String>,
)

internal fun OnboardingRepositoryDetection.describeForComment(): String = buildString {
    append("Detected repository type: ${type.displayName}")
    if (matchedMarkers.isEmpty()) {
        append(" (no repo markers matched)")
    } else {
        append(" via ${matchedMarkers.joinToString(separator = ", ")}")
    }
}

internal fun OnboardingRepositoryDetection.describeForSummary(): String = buildString {
    append(type.displayName)
    if (matchedMarkers.isNotEmpty()) {
        append(" (matched ${matchedMarkers.joinToString(separator = ", ")})")
    }
}

internal fun detectOnboardingRepositoryType(projectRoot: Path): OnboardingRepositoryDetection {
    val matchedDetections = buildList {
        if (projectRoot.resolve("package.json").isRegularFile()) {
            add(OnboardingRepositoryType.NODE to "package.json")
        }
        if (projectRoot.resolve("go.mod").isRegularFile()) {
            add(OnboardingRepositoryType.GO to "go.mod")
        }
        findDotnetMarker(projectRoot)?.let { marker ->
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

private fun findDotnetMarker(projectRoot: Path): String? =
    Files.walk(projectRoot, DOTNET_MARKER_SEARCH_DEPTH).use { stream ->
        stream
            .asSequence()
            .filter(Files::isRegularFile)
            .map(projectRoot::relativize)
            .map(Path::toString)
            .filter { relativePath ->
                relativePath.endsWith(".sln") || relativePath.endsWith(".csproj")
            }
            .sorted()
            .firstOrNull()
    }

private const val DOTNET_MARKER_SEARCH_DEPTH = 6
