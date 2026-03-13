package io.github.lmliam.microsmith.cli.init

import java.nio.file.Path

internal object ScalaOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> =
        JvmOnboardingMarkerFinder.find(projectRoot, SCALA_BUILD_MARKERS, ::isSupportedScalaSourceRoot)

    private fun isSupportedScalaSourceRoot(relativeDirectory: Path): Boolean =
        relativeDirectory.endsWith(SCALA_MAIN_SOURCE_ROOT_MARKER)
}

private val SCALA_MAIN_SOURCE_ROOT_MARKER = Path.of("src/main/scala")
