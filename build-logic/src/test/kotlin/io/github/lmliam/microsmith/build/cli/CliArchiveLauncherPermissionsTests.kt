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

class CliArchiveLauncherPermissionsTests :
    StringSpec({
        "accepts executable launchers in both archive formats" {
            withArchives("755", "755") { zip, tar ->
                verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY)
            }
        }

        "rejects a ZIP archive with a non-executable Unix launcher" {
            withArchives("644", "755") { zip, tar ->
                shouldThrow<GradleException> { verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY) }
            }
        }

        "rejects a TAR archive with a non-executable Unix launcher" {
            withArchives("755", "644") { zip, tar ->
                shouldThrow<GradleException> { verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY) }
            }
        }

        "rejects an execute-only launcher that does not match the release permission policy" {
            withArchives("100", "100") { zip, tar ->
                shouldThrow<GradleException> { verifyCliArchiveLauncherPermissions(zip, tar, LAUNCHER_ENTRY) }
            }
        }
    })

private fun withArchives(zipMode: String, tarMode: String, block: (File, File) -> Unit) {
    val root = Files.createTempDirectory("cli-archive-permissions").toFile()
    try {
        val zip = File(root, "cli.zip")
        val tar = File(root, "cli.tar.gz")
        ZipOutputStream(zip).use { output ->
            output.putNextEntry(ZipEntry(LAUNCHER_ENTRY).apply { unixMode = zipMode.toInt(8) })
            output.closeEntry()
        }
        TarOutputStream(GZIPOutputStream(tar.outputStream())).use { output ->
            output.putNextEntry(
                TarEntry(LAUNCHER_ENTRY).apply {
                    mode = tarMode.toInt(8)
                    size = 0
                }
            )
            output.closeEntry()
        }
        block(zip, tar)
    } finally {
        root.deleteRecursively()
    }
}
