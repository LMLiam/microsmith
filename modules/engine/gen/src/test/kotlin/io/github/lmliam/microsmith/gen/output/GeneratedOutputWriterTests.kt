package io.github.lmliam.microsmith.gen.output

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.FileSystems
import java.util.concurrent.Executors
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalPathApi::class)
class GeneratedOutputWriterTests :
    StringSpec(
        {
            "write overwrites generated files inside an existing routed output directory" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val outputRoot = fileSystem.getPath("/repo-root")
                    val existingProtoFile = outputRoot.resolve("services/UserService/UserCreated.proto")
                    existingProtoFile.parent.createDirectories()
                    existingProtoFile.writeText("stale")

                    GeneratedOutputWriter(GeneratedOutputPathResolver(fileSystem), UnconfinedTestDispatcher())
                        .write(
                            outputs =
                                listOf(
                                    GeneratedFile(
                                        relativePath = RelativePath.of("UserCreated.proto"),
                                        contents = "fresh".toByteArray(),
                                        outputRoot = RelativePath.of("services", "UserService"),
                                    ),
                                ),
                            space = DirectorySpace(outputRoot),
                        )

                    existingProtoFile.readText() shouldBe "fresh"
                }
            }

            "write creates missing directories for nested output paths" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val outputRoot = fileSystem.getPath("/repo-root")

                    GeneratedOutputWriter(GeneratedOutputPathResolver(fileSystem), UnconfinedTestDispatcher())
                        .write(
                            outputs =
                                listOf(
                                    GeneratedFile(
                                        relativePath = RelativePath.of("Generated", "Contracts", "Models.cs"),
                                        contents = "models".toByteArray(),
                                        outputRoot = RelativePath.of("dotnet", "Platform"),
                                    ),
                                ),
                            space = DirectorySpace(outputRoot),
                        )

                    outputRoot.resolve("dotnet/Platform/Generated/Contracts/Models.cs").readText() shouldBe "models"
                }
            }

            "write stores the exact bytes of an output" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val outputRoot = fileSystem.getPath("/repo-root")
                    val bytes = byteArrayOf(0, 1, 2, -1, -128, 127)

                    GeneratedOutputWriter(GeneratedOutputPathResolver(fileSystem), UnconfinedTestDispatcher())
                        .write(
                            outputs = listOf(GeneratedFile(RelativePath.of("blob.bin"), bytes)),
                            space = DirectorySpace(outputRoot),
                        )

                    outputRoot.resolve("blob.bin").readBytes().toList() shouldBe bytes.toList()
                }
            }

            "write creates the output directory when there are no outputs" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val outputRoot = fileSystem.getPath("/repo-root")

                    GeneratedOutputWriter(GeneratedOutputPathResolver(fileSystem), UnconfinedTestDispatcher())
                        .write(outputs = emptyList(), space = DirectorySpace(outputRoot))

                    outputRoot.isDirectory() shouldBe true
                }
            }

            "write stores every file when many outputs share their parent directories" {
                val outputRoot = createTempDirectory("microsmith-writer-")
                val outputs =
                    (0 until 200).map { index ->
                        GeneratedFile(
                            RelativePath.of("group${index % 5}", "nested", "file$index.txt"),
                            "contents$index".toByteArray(),
                        )
                    }

                val executor = Executors.newFixedThreadPool(8)
                executor.asCoroutineDispatcher().use { dispatcher ->
                    GeneratedOutputWriter(GeneratedOutputPathResolver(FileSystems.getDefault()), dispatcher)
                        .write(outputs, DirectorySpace(outputRoot))
                }

                outputs.forEach { output ->
                    outputRoot.resolve(output.relativePath.toString()).readBytes().toList() shouldBe
                        output.contents.toList()
                }
            }

            "write stops before reaching every output when the run is cancelled" {
                runTest {
                    val outputRoot = createTempDirectory("microsmith-writer-")
                    val outputs =
                        (0 until 6).map { index ->
                            GeneratedFile(
                                RelativePath.of("file$index.txt"),
                                "contents$index".toByteArray(),
                                RelativePath.of("root$index"),
                            )
                        }
                    lateinit var run: Job
                    val cancelWhenThirdIsReached =
                        object : AbstractList<GeneratedFile>() {
                            override val size = outputs.size

                            override fun get(index: Int) = outputs[index]

                            override fun iterator() = sequence {
                                outputs.forEachIndexed { index, output ->
                                    if (index == 2) run.cancel()
                                    yield(output)
                                }
                            }
                                .iterator()
                        }

                    run = launch {
                        GeneratedOutputWriter(
                                GeneratedOutputPathResolver(FileSystems.getDefault()),
                                UnconfinedTestDispatcher(),
                            )
                            .write(cancelWhenThirdIsReached, DirectorySpace(outputRoot))
                    }
                    run.join()

                    run.isCancelled shouldBe true
                    outputRoot.resolve("root5").exists() shouldBe false
                    outputRoot.walk().count { it.isRegularFile() } shouldBe 0
                }
            }

            "outputRoots lists each distinct output root once in sorted order" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val space = DirectorySpace(fileSystem.getPath("/repo-root"))
                    val outputs =
                        listOf(
                            GeneratedFile(RelativePath.of("B.cs"), byteArrayOf(), RelativePath.of("services", "B")),
                            GeneratedFile(RelativePath.of("A.cs"), byteArrayOf(), RelativePath.of("services", "A")),
                            GeneratedFile(RelativePath.of("A2.cs"), byteArrayOf(), RelativePath.of("services", "A")),
                            GeneratedFile(RelativePath.of("root.txt"), byteArrayOf()),
                        )

                    GeneratedOutputWriter(GeneratedOutputPathResolver(fileSystem), UnconfinedTestDispatcher())
                        .outputRoots(outputs, space) shouldBe
                        listOf(
                            fileSystem.getPath("/repo-root"),
                            fileSystem.getPath("/repo-root/services/A"),
                            fileSystem.getPath("/repo-root/services/B"),
                        )
                }
            }
        },
    )
