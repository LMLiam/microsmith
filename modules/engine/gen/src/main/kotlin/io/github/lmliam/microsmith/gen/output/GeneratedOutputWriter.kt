package io.github.lmliam.microsmith.gen.output

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.RunScope
import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@Inject
@SingleIn(RunScope::class)
internal class GeneratedOutputWriter(
    private val pathResolver: GeneratedOutputPathResolver,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun write(outputs: List<GeneratedFile>, space: FileSpace) {
        withContext(ioDispatcher) {
            pathResolver.resolve(space, RelativePath.ROOT)

            outputs
                .map { output ->
                    ensureActive()
                    pathResolver.resolve(space, output) to output.contents
                }
                .map { (target, contents) ->
                    async {
                        target.parent?.let(Files::createDirectories)
                        Files.write(target, contents)
                    }
                }
                .awaitAll()
        }
    }

    fun outputRoots(outputs: List<GeneratedFile>, space: FileSpace): List<Path> =
        outputs.map { output -> pathResolver.resolveRoot(space.root, output.outputRoot) }.distinct().sorted()
}
