package me.liam.microsmith.cli.init

import java.nio.file.Path

internal object JavaOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> = JvmOnboardingMarkerFinder.find(projectRoot, ::isSupportedJavaSourceRoot)

    private fun isSupportedJavaSourceRoot(relativeDirectory: Path): Boolean =
        JAVA_SOURCE_ROOT_MARKERS.any(relativeDirectory::endsWith)
}

private val JAVA_SOURCE_ROOT_MARKERS = listOf(
    Path.of("src/main/java"),
    Path.of("src/test/java"),
)
