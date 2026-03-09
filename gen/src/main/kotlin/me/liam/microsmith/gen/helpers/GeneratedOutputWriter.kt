package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import java.nio.file.Files

internal class GeneratedOutputWriter(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun write(outputs: List<GeneratedFile>, space: FileSpace) {
        withContext(ioDispatcher) {
            outputs.forEach { output ->
                val target = GeneratedOutputPathResolver.resolve(space, output.relativePath)
                target.parent?.let(Files::createDirectories)
                Files.write(target, output.contents)
            }
        }
    }
}
