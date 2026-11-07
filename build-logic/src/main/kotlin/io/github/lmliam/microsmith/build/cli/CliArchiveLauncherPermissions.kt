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
    zipArchive.verifyLauncherPermissions(launcherEntry, zipArchive.readZipEntryMode(launcherEntry))
    tarArchive.verifyLauncherPermissions(launcherEntry, tarArchive.readTarGzEntryMode(launcherEntry))
}

private fun File.readZipEntryMode(entryPath: String): Int =
    ZipFile(this).use { zip ->
        zip.getEntry(entryPath)?.unixMode
            ?: throw missingLauncherException(entryPath)
    }

private fun File.readTarGzEntryMode(entryPath: String): Int =
    TarInputStream(GZIPInputStream(inputStream().buffered())).use { tar ->
        generateSequence { tar.nextEntry }
            .firstOrNull { it.name == entryPath }
            ?.mode
            ?: throw missingLauncherException(entryPath)
    }

private fun File.verifyLauncherPermissions(
    launcherEntry: String,
    rawMode: Int,
) {
    val permissions = rawMode and UNIX_PERMISSION_MASK

    if (permissions != CLI_EXECUTABLE_FILE_MODE) {
        throw GradleException(
            "Unix launcher '$launcherEntry' in '$name' has mode ${permissions.toOctalMode()}; " +
                "expected ${CLI_EXECUTABLE_FILE_MODE.toOctalMode()}.",
        )
    }
}

private fun File.missingLauncherException(launcherEntry: String): GradleException =
    GradleException("Missing Unix launcher '$launcherEntry' in '$name'.")

private fun Int.toOctalMode(): String = "0" + toString(radix = 8)
