package me.liam.microsmith.gen.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path

class TemporaryDirectory private constructor(override val root: Path) : FileSpace, Closeable {
    override fun close() {
        root.toFile().deleteRecursively()
    }

    companion object {
        suspend fun create(
            prefix: String = ".microsmith-gen-temp-", ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ) = withContext(ioDispatcher) {
            TemporaryDirectory(Files.createTempDirectory(prefix + System.currentTimeMillis()))
        }
    }
}