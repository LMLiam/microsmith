package io.github.lmliam.microsmith.cli.ide

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal class ManagedIdeHelperFileWriter {
    fun ensureManagedDirectory(path: Path) {
        when {
            managedPathExists(path) && isManagedDirectory(path) -> return

            managedPathExists(path) ->
                throw IdeHelperConflictException("IDE helper directory '$path' exists but is not a directory.")

            else -> Files.createDirectory(path)
        }
    }

    fun writeFileIfChanged(path: Path, content: String): Boolean {
        val normalizedContent = content.replace("\r\n", "\n")
        when {
            managedPathExists(path) && !isManagedRegularFile(path) ->
                throw IdeHelperConflictException("IDE helper path '$path' exists but is not a regular file.")

            managedFileContentMatches(path, normalizedContent) -> return false
        }

        Files.writeString(path, normalizedContent, StandardCharsets.UTF_8)
        return true
    }

    private fun managedPathExists(path: Path): Boolean = Files.exists(path, LinkOption.NOFOLLOW_LINKS)

    private fun managedFileContentMatches(path: Path, normalizedContent: String): Boolean = try {
        if (!isManagedRegularFile(path)) {
            false
        } else {
            Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n") == normalizedContent
        }
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun isManagedDirectory(path: Path): Boolean = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)

    private fun isManagedRegularFile(path: Path): Boolean = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
}
