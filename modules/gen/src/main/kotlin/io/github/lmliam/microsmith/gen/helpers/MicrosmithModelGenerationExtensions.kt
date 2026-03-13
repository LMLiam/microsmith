package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.dsl.core.MicrosmithModel
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.github.lmliam.microsmith.gen.files.FileSpace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path

suspend fun MicrosmithModel.generate(finalDir: FileSpace) {
    MicrosmithGenerationRunner().generate(this, finalDir)
}

suspend fun MicrosmithModel.generateTo(outputDir: Path, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    val directorySpace = withContext(ioDispatcher) { DirectorySpace.from(outputDir) }
    generate(directorySpace)
}
