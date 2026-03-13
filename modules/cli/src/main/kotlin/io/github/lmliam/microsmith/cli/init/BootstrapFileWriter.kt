package io.github.lmliam.microsmith.cli.init

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal class BootstrapFileWriter {
    fun write(path: Path, content: String, force: Boolean): BootstrapFileWriteResult = when {
        managedPathExists(path) && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) -> {
            if (force && managedFileContentDiffers(path, content)) {
                Files.writeString(path, content, StandardCharsets.UTF_8)
                BootstrapFileWriteResult.Overwritten(path)
            } else {
                BootstrapFileWriteResult.Preserved(path)
            }
        }

        managedPathExists(path) ->
            throw InitConflictException("Bootstrap path '$path' exists but is not a regular file.")

        else -> {
            Files.createDirectories(path.parent)
            Files.writeString(path, content, StandardCharsets.UTF_8)
            BootstrapFileWriteResult.Created(path)
        }
    }

    private fun managedFileContentDiffers(path: Path, expectedContent: String): Boolean = try {
        val existingContent = Files.readString(path, StandardCharsets.UTF_8)
        existingContent.normalizeLineEndings() != expectedContent.normalizeLineEndings()
    } catch (_: IOException) {
        true
    } catch (_: SecurityException) {
        true
    }
}

private fun managedPathExists(path: Path): Boolean = Files.exists(path, LinkOption.NOFOLLOW_LINKS)

private fun String.normalizeLineEndings(): String = replace("\r\n", "\n")
