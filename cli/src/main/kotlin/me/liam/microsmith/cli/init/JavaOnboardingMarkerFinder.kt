package me.liam.microsmith.cli.init

import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

internal object JavaOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> {
        val matchedSourceMarkers =
            SOURCE_MARKERS.filter { markerPath ->
                projectRoot.resolve(markerPath).isDirectory()
            }
        if (matchedSourceMarkers.isEmpty()) {
            return emptyList()
        }

        val matchedBuildMarkers =
            BUILD_MARKERS.filter { markerFileName ->
                projectRoot.resolve(markerFileName).isRegularFile()
            }
        return (matchedBuildMarkers + matchedSourceMarkers).sorted()
    }
}

private val BUILD_MARKERS = listOf(
    "pom.xml",
    "build.gradle",
    "build.gradle.kts",
    "settings.gradle",
    "settings.gradle.kts",
)

private val SOURCE_MARKERS = listOf(
    "src/main/java",
    "src/test/java",
)
