package io.github.lmliam.microsmith.gen.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

class TemporaryDirectory private constructor(override val root: Path) :
    FileSpace,
    Closeable {
    override fun close() {
        runCatching {
            Files.walk(root).use { paths ->
                paths
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            }
        }.onFailure { error ->
            System.err.println("Failed to cleanup temporary directory '$root': ${error.message}")
        }
    }

    companion object {
        suspend fun create(
            prefix: String = ".microsmith-gen-temp-",
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = withContext(ioDispatcher) {
            TemporaryDirectory(Files.createTempDirectory(prefix + System.currentTimeMillis()))
        }
    }
}
