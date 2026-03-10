package me.liam.microsmith.cli.init

import java.nio.file.Files
import java.nio.file.Path

internal object RubyOnboardingMarkerFinder {
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
