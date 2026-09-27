package io.github.lmliam.microsmith.gen.output

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.gen.MicrosmithGenerationScope
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files

@Inject
@SingleIn(MicrosmithGenerationScope::class)
internal class GeneratedOutputWriter(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun write(outputs: List<GeneratedFile>, space: FileSpace) {
        withContext(ioDispatcher) {
            outputs.forEach { output ->
                val target = GeneratedOutputPathResolver.resolve(space, output)

                target.parent?.let(Files::createDirectories)

                Files.write(target, output.contents)
            }
        }
    }
}
