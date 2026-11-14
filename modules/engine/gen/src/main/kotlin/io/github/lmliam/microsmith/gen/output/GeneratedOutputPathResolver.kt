package io.github.lmliam.microsmith.gen.output

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

@Inject
internal class GeneratedOutputPathResolver(private val fileSystem: FileSystem) {
    fun resolve(space: FileSpace, output: GeneratedFile): Path {
        val normalizedRoot = space.root.toAbsolutePath().normalize()
        ensureOutputRootIsSafe(normalizedRoot)

        val routedRoot = resolveRoot(normalizedRoot, output.outputRoot)
        val routedSpace =
            if (routedRoot == normalizedRoot) {
                space
            } else {
                DirectorySpace.from(routedRoot)
            }

        return resolve(routedSpace, output.relativePath)
    }

    fun resolve(space: FileSpace, relativePath: RelativePath): Path {
        val normalizedRoot = space.root.toAbsolutePath().normalize()
        ensureOutputRootIsSafe(normalizedRoot)

        requireNoSymlinkTraversal(normalizedRoot, relativePath)

        return normalizedRoot.resolve(fileSystem.pathOf(relativePath))
    }

    fun resolveRoot(root: Path, outputRoot: RelativePath): Path {
        val normalizedRoot = root.toAbsolutePath().normalize()
        ensureOutputRootIsSafe(normalizedRoot)

        requireNoSymlinkTraversal(normalizedRoot, outputRoot)

        return normalizedRoot.resolve(fileSystem.pathOf(outputRoot))
    }

    private fun ensureOutputRootIsSafe(root: Path) {
        if (Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            require(Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) { "Output root '$root' must be a directory." }
            require(!Files.isSymbolicLink(root)) { "Output root '$root' must not be a symbolic link." }
            return
        }

        Files.createDirectories(root)
    }

    private fun requireNoSymlinkTraversal(root: Path, relativePath: RelativePath) {
        var current = root
        val segments = relativePath.segments
        for ((index, segment) in segments.withIndex()) {
            current = current.resolve(segment)

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
