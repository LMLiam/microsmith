package io.github.lmliam.microsmith.gen

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CyclicBarrier
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private class ConcurrentRuns(val firstOutput: Path, val secondOutput: Path, val diagnostics: RecordingDiagnosticSink)

private suspend fun runTwoGenerationsAtOnce(dispatcher: CoroutineDispatcher = Dispatchers.IO): ConcurrentRuns {
    val diagnostics = RecordingDiagnosticSink()
    val generator = testGenerator(diagnostics, dispatcher)
    val firstOutput = Files.createTempDirectory("microsmith-concurrent-first-")
    val secondOutput = Files.createTempDirectory("microsmith-concurrent-second-")
    ResolutionRendezvous.barrier = CyclicBarrier(2)

    try {
        coroutineScope {
            listOf(
                    async(dispatcher) {
                        generator.generateTo(
                            labelledModel(),
                            firstOutput,
                            labelClassLoader(FirstLabelResolver::class),
                        )
                    },
                    async(dispatcher) {
                        generator.generateTo(
                            labelledModel(),
                            secondOutput,
                            labelClassLoader(SecondLabelResolver::class),
                        )
                    },
                )
                .awaitAll()
        }
    } finally {
        ResolutionRendezvous.barrier = null
    }

    return ConcurrentRuns(firstOutput, secondOutput, diagnostics)
}

class ConcurrentGenerationTests :
    StringSpec(
        {
            "concurrent generate calls write only their own files from their own class loader" {
                val runs = runTwoGenerationsAtOnce()

                listOf(runs.firstOutput to "first", runs.secondOutput to "second").forEach { (output, label) ->
                    output.listDirectoryEntries().map { it.name } shouldContainExactlyInAnyOrder
                        listOf("label.txt", "label.bin")
                    output.resolve("label.txt").readText() shouldBe label
                    Files.readAllBytes(output.resolve("label.bin")).decodeToString() shouldBe label
                }
            }

            "concurrent generate calls report their own completion diagnostics" {
                val runs = runTwoGenerationsAtOnce()

                runs.diagnostics.diagnostics.map { it.message } shouldContainExactlyInAnyOrder
                    listOf(
                        "Generated all files in ${runs.firstOutput.toAbsolutePath().normalize()}",
                        "Generated all files in ${runs.secondOutput.toAbsolutePath().normalize()}",
                    )
            }
        },
    )
