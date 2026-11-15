package io.github.lmliam.microsmith.gen.output

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.test.UnconfinedTestDispatcher

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
