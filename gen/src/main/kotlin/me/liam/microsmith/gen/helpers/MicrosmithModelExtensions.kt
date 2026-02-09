package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.gen.core.GeneratorRegistry
import me.liam.microsmith.gen.core.GeneratorRegistry.getGenerator
import me.liam.microsmith.gen.files.DirectorySpace
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import me.liam.microsmith.gen.files.TemporaryDirectory
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

suspend fun MicrosmithModel.generate(finalDir: FileSpace) =
    coroutineScope {
        GeneratorRegistry.load()

        val outputs =
            TemporaryDirectory.create().use { tempSpace ->
                val generated = runGenerators(finalDir)
                requireUniqueRelativePaths(generated)
                writeOutputs(generated, tempSpace)
                generated
            }

        writeOutputs(outputs, finalDir)
        println("Generated all files in ${finalDir.root}")
    }

suspend fun MicrosmithModel.generateTo(
    outputDir: Path,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val directorySpace = withContext(ioDispatcher) { DirectorySpace.from(outputDir) }
    generate(directorySpace)
}

private fun requireUniqueRelativePaths(outputs: List<GeneratedFile>) {
    val duplicates =
        outputs
            .groupBy { it.relativePath.normalize().toString() }
            .filterValues { it.size > 1 }

    require(duplicates.isEmpty()) {
        val details = duplicates.keys.sorted().joinToString(", ")
        "Duplicate output file paths detected: $details"
    }
}

private suspend fun MicrosmithModel.runGenerators(space: FileSpace) =
    coroutineScope {
        extensions()
            .map { ext ->
                async {
                    val gen =
                        ext.getGenerator() ?: run {
                            println("No generator found for ${ext::class.simpleName}")
                            return@async emptyList()
                        }
                    gen.run { ext.generate(space) }.also {
                        println("Generated ${it.size} files for ${ext::class.simpleName} in ${space.root}")
                    }
                }
            }.awaitAll()
            .flatten()
    }

private suspend fun writeOutputs(
    outputs: List<GeneratedFile>,
    space: FileSpace,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) = withContext(ioDispatcher) {
    outputs.forEach { out ->
        val target = resolveTargetPath(space, out.relativePath)
        target.parent?.let(Files::createDirectories)
        Files.write(target, out.contents)
    }
}

internal fun resolveTargetPath(
    space: FileSpace,
    relativePath: Path
): Path {
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

private fun requireNoSymlinkTraversal(
    root: Path,
    relativePath: Path
) {
    var current = root
    val segments = relativePath.toList()
    segments.forEachIndexed { index, segment ->
        current = current.resolve(segment.toString())

        if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            return@forEachIndexed
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
