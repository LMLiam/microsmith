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
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import me.liam.microsmith.gen.files.TemporaryDirectory
import java.nio.file.Files

suspend fun MicrosmithModel.generate(finalDir: FileSpace) =
    coroutineScope {
        GeneratorRegistry.load()

        val outputs =
            TemporaryDirectory.create().use { tempSpace ->
                val generated = runGenerators(tempSpace)
                requireUniqueRelativePaths(generated)
                writeOutputs(generated, tempSpace)
                generated
            }

        writeOutputs(outputs, finalDir)
        println("Generated all files in ${finalDir.root}")
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
        val target = space.root.resolve(out.relativePath)
        target.parent?.let(Files::createDirectories)
        Files.write(target, out.contents)
    }
}
