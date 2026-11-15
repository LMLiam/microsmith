package io.github.lmliam.microsmith.cli.init.profile

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

internal object RubyOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> =
        try {
            findRootGemspecs(projectRoot)
        } catch (_: IOException) {
            emptyList()
        } catch (_: UncheckedIOException) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

    private fun findRootGemspecs(projectRoot: Path): List<String> =
        projectRoot
            .listDirectoryEntries()
            .filter { candidate -> candidate.isRegularFile() && candidate.fileName.toString().endsWith(".gemspec") }
            .map { candidate -> candidate.fileName.toString() }
            .sorted()
}
