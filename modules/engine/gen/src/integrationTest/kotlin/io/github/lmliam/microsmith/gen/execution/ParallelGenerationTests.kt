package io.github.lmliam.microsmith.gen.execution

import io.github.lmliam.microsmith.gen.generateWithPlugins
import io.github.lmliam.microsmith.gen.generatedBytes
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

private suspend fun generateWithThreads(threads: Int): Path {
    val executor = Executors.newFixedThreadPool(threads)

    try {
        return generateWithPlugins("alpha", "bulk", dispatcher = executor.asCoroutineDispatcher())
    } finally {
        executor.shutdown()
    }
}

class ParallelGenerationTests :
    StringSpec(
        {
            "parallel rendering and writing give the same bytes as a sequential run" {
                val sequential = generatedBytes(generateWithThreads(threads = 1))

                val parallel = generatedBytes(generateWithThreads(threads = 8))

                sequential shouldHaveSize 65
                parallel shouldBe sequential
            }
        },
    )
