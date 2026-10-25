package io.github.lmliam.microsmith.build.cli

import java.io.File
import java.util.zip.GZIPInputStream
import org.apache.tools.tar.TarInputStream
import org.apache.tools.zip.ZipFile
import org.gradle.api.GradleException

internal const val CLI_EXECUTABLE_FILE_MODE = 0b111_101_101
private const val UNIX_PERMISSION_MASK = 0b111_111_111_111

internal fun verifyCliArchiveLauncherPermissions(
    zipArchive: File,
    tarArchive: File,
    launcherEntry: String,
) {
    zipArchive.requireLauncherMode(launcherEntry, readZipLauncherMode(zipArchive, launcherEntry))
    tarArchive.requireLauncherMode(launcherEntry, readTarGzLauncherMode(tarArchive, launcherEntry))
}

private fun readZipLauncherMode(
    archive: File,
    launcherEntry: String,
): Int =
    ZipFile(archive).use { zip ->
        zip.getEntry(launcherEntry)?.unixMode
            ?: throw missingLauncher(archive, launcherEntry)
    }

private fun readTarGzLauncherMode(
    archive: File,
    launcherEntry: String,
): Int =
    TarInputStream(GZIPInputStream(archive.inputStream())).use { tar ->
        generateSequence { tar.nextEntry }
            .firstOrNull { it.name == launcherEntry }
            ?.mode
            ?: throw missingLauncher(archive, launcherEntry)
    }

private fun File.requireLauncherMode(
    launcherEntry: String,
    mode: Int,
) {
    val permissions = mode and UNIX_PERMISSION_MASK

    if (permissions != CLI_EXECUTABLE_FILE_MODE) {
        throw GradleException(
            "Unix launcher '$launcherEntry' in $name has mode ${permissions.toOctal()}; " +
                "expected ${CLI_EXECUTABLE_FILE_MODE.toOctal()}.",
        )
    }
}

private fun missingLauncher(
    archive: File,
    launcherEntry: String,
): GradleException = GradleException("Missing Unix launcher '$launcherEntry' in ${archive.name}.")

private fun Int.toOctal(): String = toString(radix = 8)
