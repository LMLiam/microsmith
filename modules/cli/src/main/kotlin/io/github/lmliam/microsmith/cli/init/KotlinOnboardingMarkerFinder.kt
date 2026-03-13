package io.github.lmliam.microsmith.cli.init

import java.nio.file.Path

internal object KotlinOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> =
        JvmOnboardingMarkerFinder.find(projectRoot, JVM_LANGUAGE_BUILD_MARKERS, ::isSupportedKotlinSourceRoot)

    private fun isSupportedKotlinSourceRoot(relativeDirectory: Path): Boolean {
        if (relativeDirectory.nameCount < MINIMUM_KOTLIN_SOURCE_ROOT_DEPTH) {
            return false
        }

        if (relativeDirectory.fileName.toString() != KOTLIN_SOURCE_DIRECTORY_NAME) {
            return false
        }

        val sourceDirectoryName =
            relativeDirectory.getName(relativeDirectory.nameCount - SOURCE_ROOT_PREFIX_DEPTH).toString()
        if (sourceDirectoryName != SOURCE_DIRECTORY_NAME) {
            return false
        }

        val sourceSetName =
            relativeDirectory.getName(relativeDirectory.nameCount - SOURCE_SET_NAME_OFFSET).toString()
        return sourceSetName == MAIN_SOURCE_SET_NAME ||
            sourceSetName == TEST_SOURCE_SET_NAME ||
            sourceSetName.endsWith(MAIN_SOURCE_SET_SUFFIX) ||
            sourceSetName.endsWith(TEST_SOURCE_SET_SUFFIX)
    }
}

private const val MINIMUM_KOTLIN_SOURCE_ROOT_DEPTH = 3
private const val SOURCE_ROOT_PREFIX_DEPTH = 3
private const val SOURCE_SET_NAME_OFFSET = 2

private const val SOURCE_DIRECTORY_NAME = "src"
private const val KOTLIN_SOURCE_DIRECTORY_NAME = "kotlin"
private const val MAIN_SOURCE_SET_NAME = "main"
private const val TEST_SOURCE_SET_NAME = "test"
private const val MAIN_SOURCE_SET_SUFFIX = "Main"
private const val TEST_SOURCE_SET_SUFFIX = "Test"
