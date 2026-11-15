package io.github.lmliam.microsmith.gen.render

import io.github.lmliam.microsmith.artifact.ArtifactRenderer
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembly
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssemblyService
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.gen.ArtifactRendererRegistry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

private class RenderHook(private val onRender: (String) -> Unit) : ArtifactRenderer<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

    override fun render(artifact: TextFileArtifact): GeneratedFile {
        onRender(artifact.id.relativePath.toString())

        return GeneratedFile(artifact.id.relativePath, artifact.contents.toByteArray())
    }
}

private fun assemblyOf(vararg paths: String): ArtifactAssembly =
    ArtifactAssemblyService(listOf(TextFileArtifactAssembler()))
        .assemble(paths.map { TextFileArtifactContribution(TextFileArtifactId(RelativePath.of(it)), it) })

class ArtifactRenderingServiceTests :
    StringSpec(
        {
            val parallelDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

            afterSpec { parallelDispatcher.close() }

            "artifacts are rendered concurrently" {
                val barrier = CyclicBarrier(2)
                val service =
                    ArtifactRenderingService(
                        ArtifactRendererRegistry(listOf(RenderHook { barrier.await(5, TimeUnit.SECONDS) })),
                        parallelDispatcher,
                    )

                service.render(assemblyOf("a.txt", "b.txt")).map { it.relativePath.toString() } shouldBe
                    listOf("a.txt", "b.txt")
            }

            "files are returned in artifact order when a later artifact finishes first" {
                val service =
                    ArtifactRenderingService(
                        ArtifactRendererRegistry(listOf(RenderHook { if (it == "a.txt") Thread.sleep(100) })),
                        parallelDispatcher,
                    )

                service.render(assemblyOf("a.txt", "b.txt", "c.txt")).map { it.relativePath.toString() } shouldBe
                    listOf("a.txt", "b.txt", "c.txt")
            }

            "a cancelled run renders no further artifacts" {
                runTest {
                    val rendered = ConcurrentLinkedQueue<String>()
                    lateinit var run: Job
                    val service =
                        ArtifactRenderingService(
                            ArtifactRendererRegistry(
                                listOf(
                                    RenderHook {
                                        rendered += it
                                        run.cancel()
                                    },
                                ),
                            ),
                            StandardTestDispatcher(testScheduler),
                        )

                    run = launch(start = CoroutineStart.LAZY) { service.render(assemblyOf("a.txt", "b.txt", "c.txt")) }
                    run.start()
                    run.join()

                    run.isCancelled shouldBe true
                    rendered.toList() shouldBe listOf("a.txt")
                }
            }
        },
    )
