package io.github.lmliam.microsmith.build.cli

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import org.apache.tools.tar.TarEntry
import org.apache.tools.tar.TarOutputStream
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.GradleException

private const val LAUNCHER_ENTRY = "microsmith-cli/bin/microsmith"

private const val MODE_755 = 0b111_101_101
private const val MODE_644 = 0b110_100_100
private const val MODE_100 = 0b001_000_000

class CliArchiveLauncherPermissionsTests :
    StringSpec({

        "accepts executable launchers in both archive formats" {
            withArchives(zipMode = MODE_755, tarMode = MODE_755) { zip, tar ->
                verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY)
            }
        }

        "rejects a ZIP archive with a non-executable Unix launcher" {
            withArchives(zipMode = MODE_644, tarMode = MODE_755) { zip, tar ->
                shouldThrow<GradleException> {
                    verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY)
                }
            }
        }

        "rejects a TAR archive with a non-executable Unix launcher" {
            withArchives(zipMode = MODE_755, tarMode = MODE_644) { zip, tar ->
                shouldThrow<GradleException> {
                    verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY)
                }
            }
        }

        "rejects an execute-only launcher that does not match the release permission policy" {
            withArchives(zipMode = MODE_100, tarMode = MODE_100) { zip, tar ->
                shouldThrow<GradleException> {
                    verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY)
                }
            }
        }
    })

private fun withArchives(
    zipMode: Int,
    tarMode: Int,
    block: (zipFile: File, tarFile: File) -> Unit,
) {
    val tempDir = Files.createTempDirectory("cli-archive-permissions").toFile()
    try {
        val zipFile = tempDir.resolve("cli.zip").apply { writeZipEntry(LAUNCHER_ENTRY, zipMode) }
        val tarFile = tempDir.resolve("cli.tar.gz").apply { writeTarGzEntry(LAUNCHER_ENTRY, tarMode) }

        block(zipFile, tarFile)
    } finally {
        tempDir.deleteRecursively()
    }
}

private fun File.writeZipEntry(
    entryPath: String,
    mode: Int,
) {
    ZipOutputStream(outputStream().buffered()).use { zip ->
        zip.putNextEntry(ZipEntry(entryPath).apply { unixMode = mode })
        zip.closeEntry()
    }
}

private fun File.writeTarGzEntry(
    entryPath: String,
    mode: Int,
) {
    TarOutputStream(GZIPOutputStream(outputStream().buffered())).use { tar ->
        tar.putNextEntry(
            TarEntry(entryPath).apply {
                this.mode = mode
                size = 0
            },
        )
        tar.closeEntry()
    }
}
