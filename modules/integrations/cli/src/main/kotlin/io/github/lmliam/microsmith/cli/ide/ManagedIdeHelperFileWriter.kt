package io.github.lmliam.microsmith.cli.ide

import java.io.IOException
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

internal class ManagedIdeHelperFileWriter {
    fun ensureManagedDirectory(path: Path) {
        when {
            managedPathExists(path) && isManagedDirectory(path) -> return

            managedPathExists(path) ->
                throw IdeHelperConflictException("IDE helper directory '$path' exists but is not a directory.")

            else -> path.createDirectory()
        }
    }

    fun writeFileIfChanged(path: Path, content: String): Boolean {
        val normalizedContent = content.replace("\r\n", "\n")
        when {
            managedPathExists(path) && !isManagedRegularFile(path) ->
                throw IdeHelperConflictException("IDE helper path '$path' exists but is not a regular file.")

            managedFileContentMatches(path, normalizedContent) -> return false
        }

        path.writeText(normalizedContent)
        return true
    }

    private fun managedPathExists(path: Path): Boolean = path.exists(LinkOption.NOFOLLOW_LINKS)

    private fun managedFileContentMatches(path: Path, normalizedContent: String): Boolean =
        try {
            if (!isManagedRegularFile(path)) {
                false
            } else {
                path.readBytes().decodeToString(throwOnInvalidSequence = true).replace("\r\n", "\n") ==
                    normalizedContent
            }
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }

    private fun isManagedDirectory(path: Path): Boolean = path.isDirectory(LinkOption.NOFOLLOW_LINKS)

    private fun isManagedRegularFile(path: Path): Boolean = path.isRegularFile(LinkOption.NOFOLLOW_LINKS)
}
