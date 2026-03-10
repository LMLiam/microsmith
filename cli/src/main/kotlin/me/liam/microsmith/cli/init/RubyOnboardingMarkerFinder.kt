package me.liam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path

internal object RubyOnboardingMarkerFinder {
    fun findRootGemspecsSafely(
        projectRoot: Path,
        markerFinder: (Path) -> List<String> = ::findRootGemspecs,
    ): List<String> = try {
        markerFinder(projectRoot)
    } catch (_: IOException) {
        emptyList()
    } catch (_: UncheckedIOException) {
        emptyList()
    } catch (_: SecurityException) {
        emptyList()
    }

    fun findRootGemspecs(projectRoot: Path): List<String> {
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
