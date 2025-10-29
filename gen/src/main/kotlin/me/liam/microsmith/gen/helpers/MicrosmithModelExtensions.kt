package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.*
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

        TemporaryDirectory.create().use { tempSpace ->
            runGenerators(tempSpace)
        }

        val outputs = runGenerators(finalDir)
        writeOutputs(outputs, finalDir)
        println("✅ Generated all files in ${finalDir.root}")
    }

private suspend fun MicrosmithModel.runGenerators(tempSpace: FileSpace) =
    coroutineScope {
        extensions()
            .map { ext ->
                async {
                    val gen =
                        ext.getGenerator() ?: run {
                            println("⚠️ No generator found for ${ext::class.simpleName}")
                            return@async emptyList()
                        }
                    gen.run { ext.generate(tempSpace) }.also {
                        println("🛠️ Generated ${it.size} files for ${ext::class.simpleName} in ${tempSpace.root}")
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
    outputs.map { out ->
        val target = space.root.resolve(out.relativePath)
        Files.createDirectories(target.parent)
        Files.write(target, out.contents)
        target
    }
}