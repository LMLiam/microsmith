package io.github.lmliam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path

internal object RubyOnboardingMarkerFinder {
    fun find(projectRoot: Path): List<String> = try {
        findRootGemspecs(projectRoot)
    } catch (_: IOException) {
        emptyList()
    } catch (_: UncheckedIOException) {
        emptyList()
    } catch (_: SecurityException) {
        emptyList()
    }

    private fun findRootGemspecs(projectRoot: Path): List<String> {
        return Files.newDirectoryStream(projectRoot) { candidate ->
            Files.isRegularFile(candidate) && candidate.fileName.toString().endsWith(".gemspec")
        }.use { directoryStream ->
            directoryStream
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted()
                .toList()
        }
    }
}
