package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.gen.core.GeneratorRegistry.getGenerator
import me.liam.microsmith.gen.files.TemporaryFileSpace
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

suspend fun MicrosmithModel.generate(
    finalDir: Path,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) = coroutineScope {
    val tempDir = withContext(ioDispatcher) {
        Files.createTempDirectory(".microsmith-gen-temp")
    }
    val tempSpace = TemporaryFileSpace(tempDir)

    try {
        val allPaths = runGenerators(tempSpace)
        promoteFiles(tempDir, finalDir, ioDispatcher)
        resolveFinalPaths(allPaths, tempDir, finalDir)
    } catch (ex: Exception) {
        withContext(ioDispatcher) {
            tempDir.toFile().deleteRecursively()
        }
        throw ex
    }
}

private suspend fun MicrosmithModel.runGenerators(
    tempSpace: TemporaryFileSpace
) = coroutineScope {
    extensions().map { ext ->
        async {
            val gen = ext.getGenerator()
            gen.run { ext.generate(tempSpace) }
        }
    }
        .awaitAll()
        .flatten()
}

private suspend fun promoteFiles(
    tempDir: Path,
    finalDir: Path,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) = withContext(ioDispatcher) {
    Files.createDirectories(finalDir)
    tempDir.toFile().walkTopDown().forEach { file ->
        if (!file.isFile) return@forEach
        val rel = tempDir.relativize(file.toPath())
        val target = finalDir.resolve(rel)
        Files.createDirectories(target.parent)
        Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun resolveFinalPaths(
    allPaths: List<Path>,
    tempDir: Path,
    finalDir: Path
) = allPaths.map { finalDir.resolve(tempDir.relativize(it)) }
