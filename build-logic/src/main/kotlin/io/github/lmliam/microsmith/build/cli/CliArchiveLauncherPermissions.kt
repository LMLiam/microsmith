package io.github.lmliam.microsmith.build.cli

import java.io.File
import java.util.zip.GZIPInputStream
import org.apache.tools.tar.TarInputStream
import org.apache.tools.zip.ZipFile
import org.gradle.api.GradleException

internal const val CLI_EXECUTABLE_FILE_MODE = 0b111_101_101
private const val UNIX_PERMISSION_MASK = 0b111_111_111_111

internal fun verifyCliArchiveLauncherPermissions(zipArchive: File, tarArchive: File, launcherEntry: String) {
    ZipFile(zipArchive).use { zip ->
        val launcher =
            zip.getEntry(launcherEntry)
                ?: throw GradleException("Missing Unix launcher '$launcherEntry' in ${zipArchive.name}.")
        requireExecutableLauncher(zipArchive, launcherEntry, launcher.unixMode)
    }
    TarInputStream(GZIPInputStream(tarArchive.inputStream())).use { tar ->
        val launcher =
            generateSequence { tar.nextEntry }.firstOrNull { it.name == launcherEntry }
                ?: throw GradleException("Missing Unix launcher '$launcherEntry' in ${tarArchive.name}.")
        requireExecutableLauncher(tarArchive, launcherEntry, launcher.mode)
    }
}

private fun requireExecutableLauncher(archive: File, launcherEntry: String, mode: Int) {
    val permissions = mode and UNIX_PERMISSION_MASK
    if (permissions != CLI_EXECUTABLE_FILE_MODE) {
        throw GradleException(
            "Unix launcher '$launcherEntry' in ${archive.name} has mode ${permissions.toString(8)}; " +
                "expected ${CLI_EXECUTABLE_FILE_MODE.toString(8)}."
        )
    }
}
