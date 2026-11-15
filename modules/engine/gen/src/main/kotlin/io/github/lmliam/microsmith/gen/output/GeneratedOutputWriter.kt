package io.github.lmliam.microsmith.gen.output

import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.RunScope
import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlinx.coroutines.CoroutineDispatcher
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

            outputs.forEach { output ->
                val target = pathResolver.resolve(space, output)

                target.parent?.let(Path::createDirectories)

                target.writeBytes(output.contents)
            }
        }
    }

    fun outputRoots(outputs: List<GeneratedFile>, space: FileSpace): List<Path> =
        outputs.map { output -> pathResolver.resolveRoot(space.root, output.outputRoot) }.distinct().sorted()
}
