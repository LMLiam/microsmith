package io.github.lmliam.microsmith.cli.init.files

import io.github.lmliam.microsmith.cli.init.InitConflictException
import java.io.IOException
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

internal class BootstrapFileWriter {
    fun write(path: Path, content: String, force: Boolean): BootstrapFileWriteResult =
        when {
            managedPathExists(path) && path.isRegularFile(LinkOption.NOFOLLOW_LINKS) -> {
                if (force && managedFileContentDiffers(path, content)) {
                    path.writeText(content)
                    BootstrapFileWriteResult.Overwritten(path)
                } else {
                    BootstrapFileWriteResult.Preserved(path)
                }
            }

            managedPathExists(path) ->
                throw InitConflictException("Bootstrap path '$path' exists but is not a regular file.")

            else -> {
                path.parent.createDirectories()
                path.writeText(content)
                BootstrapFileWriteResult.Created(path)
            }
        }

    private fun managedFileContentDiffers(path: Path, expectedContent: String): Boolean =
        try {
            val existingContent = path.readBytes().decodeToString(throwOnInvalidSequence = true)
            existingContent.normalizeLineEndings() != expectedContent.normalizeLineEndings()
        } catch (_: IOException) {
            true
        } catch (_: SecurityException) {
            true
        }
}

private fun managedPathExists(path: Path): Boolean = path.exists(LinkOption.NOFOLLOW_LINKS)

private fun String.normalizeLineEndings(): String = replace("\r\n", "\n")
