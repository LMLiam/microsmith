package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.gen.files.DirectorySpace
import me.liam.microsmith.gen.files.FileSpace
import java.nio.file.Path

suspend fun MicrosmithModel.generate(finalDir: FileSpace) {
    MicrosmithGenerationRunner().generate(this, finalDir)
}

suspend fun MicrosmithModel.generateTo(outputDir: Path, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    val directorySpace = withContext(ioDispatcher) { DirectorySpace.from(outputDir) }
    generate(directorySpace)
}
