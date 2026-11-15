package io.github.lmliam.microsmith.gen.output

import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class GeneratedOutputSymbolicLinkTests :
    StringSpec(
        {
            val fileSystem = FileSystems.getDefault()

            "resolve rejects an output path that traverses a symbolic link" {
                val root = Files.createTempDirectory("microsmith-paths-root-")
                val outside = Files.createTempDirectory("microsmith-paths-outside-")
                val linked = runCatching { Files.createSymbolicLink(root.resolve("link"), outside) }.isSuccess
                if (linked) {
                    val space = DirectorySpace(root)

                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputPathResolver(fileSystem).resolve(space, RelativePath.of("link", "secret.proto"))
                    }
                }
            }

            "resolve rejects a routed output root that traverses a symbolic link" {
                val root = Files.createTempDirectory("microsmith-paths-root-")
                val outside = Files.createTempDirectory("microsmith-paths-outside-")
                val linked = runCatching { Files.createSymbolicLink(root.resolve("link"), outside) }.isSuccess
                if (linked) {
                    val space = DirectorySpace(root)
                    val output =
                        GeneratedFile(
                            relativePath = RelativePath.of("secret.proto"),
                            contents = byteArrayOf(),
                            outputRoot = RelativePath.of("link", "nested"),
                        )

                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputPathResolver(fileSystem).resolve(space, output)
                    }
                }
            }

            "write does not create files outside the output root through a symbolic link" {
                val root = Files.createTempDirectory("microsmith-paths-root-")
                val outside = Files.createTempDirectory("microsmith-paths-outside-")
                val linked = runCatching { Files.createSymbolicLink(root.resolve("link"), outside) }.isSuccess
                if (linked) {
                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputWriter(GeneratedOutputPathResolver(fileSystem), UnconfinedTestDispatcher())
                            .write(
                                listOf(GeneratedFile(RelativePath.of("link", "secret.proto"), byteArrayOf(1))),
                                DirectorySpace(root),
                            )
                    }

                    Files.exists(outside.resolve("secret.proto")) shouldBe false
                }
            }
        },
    )
