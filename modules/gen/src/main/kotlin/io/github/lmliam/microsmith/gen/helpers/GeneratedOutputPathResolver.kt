package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

internal object GeneratedOutputPathResolver {
    fun resolve(space: FileSpace, relativePath: Path): Path {
        require(!relativePath.isAbsolute) {
            "Generated output path must be relative, but was '$relativePath'."
        }

        val normalizedRoot = space.root.toAbsolutePath().normalize()
        ensureOutputRootIsSafe(normalizedRoot)
        val normalizedRelativePath = relativePath.normalize()
        val target = normalizedRoot.resolve(normalizedRelativePath).normalize()

        require(target.startsWith(normalizedRoot)) {
            "Generated output path '$relativePath' escapes output root '$normalizedRoot'."
        }

        requireNoSymlinkTraversal(normalizedRoot, normalizedRelativePath)

        return target
    }

    private fun ensureOutputRootIsSafe(root: Path) {
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            require(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                "Output root '$root' must be a directory."
            }
            require(!Files.isSymbolicLink(root)) {
                "Output root '$root' must not be a symbolic link."
            }
            return
        }

        Files.createDirectories(root)
    }

    private fun requireNoSymlinkTraversal(root: Path, relativePath: Path) {
        var current = root
        val segments = relativePath.toList()
        for ((index, segment) in segments.withIndex()) {
            current = current.resolve(segment.toString())

            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                continue
            }

            require(!Files.isSymbolicLink(current)) {
                "Generated output path '$relativePath' traverses symbolic link '$current'."
            }

            val isLastSegment = index == segments.lastIndex
            if (!isLastSegment) {
                require(Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
                    "Generated output path '$relativePath' contains non-directory segment '$current'."
                }
            }
        }
    }
}
