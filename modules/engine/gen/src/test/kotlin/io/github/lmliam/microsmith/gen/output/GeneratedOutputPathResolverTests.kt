package io.github.lmliam.microsmith.gen.output

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files

class GeneratedOutputPathResolverTests :
    StringSpec(
        {
            "resolve places a relative output path under the root" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val space = DirectorySpace.from(fileSystem.getPath("/out"))

                    val resolved =
                        GeneratedOutputPathResolver(fileSystem)
                            .resolve(space, RelativePath.of("proto", "pkg", "User.proto"))

                    resolved shouldBe fileSystem.getPath("/out/proto/pkg/User.proto")
                }
            }

            "resolve does not create the output file" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val space = DirectorySpace.from(fileSystem.getPath("/out"))

                    val resolved = GeneratedOutputPathResolver(fileSystem).resolve(space, RelativePath.of("User.proto"))

                    Files.exists(resolved) shouldBe false
                }
            }

            "resolve routes an output into its nested output root" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val space = DirectorySpace.from(fileSystem.getPath("/out"))
                    val output =
                        GeneratedFile(
                            relativePath = RelativePath.of("UserService.cs"),
                            contents = byteArrayOf(),
                            outputRoot = RelativePath.of("services", "UserService"),
                        )

                    GeneratedOutputPathResolver(fileSystem).resolve(space, output) shouldBe
                        fileSystem.getPath("/out/services/UserService/UserService.cs")
                }
            }

            "resolve places an output with the root output root directly under the space" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val space = DirectorySpace.from(fileSystem.getPath("/out"))
                    val output = GeneratedFile(RelativePath.of("User.proto"), byteArrayOf())

                    GeneratedOutputPathResolver(fileSystem).resolve(space, output) shouldBe
                        fileSystem.getPath("/out/User.proto")
                }
            }

            "resolveRoot places a nested output root under the root" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val resolved =
                        GeneratedOutputPathResolver(fileSystem)
                            .resolveRoot(fileSystem.getPath("/out"), RelativePath.of("dotnet", "Platform"))

                    resolved shouldBe fileSystem.getPath("/out/dotnet/Platform")
                }
            }

            "resolveRoot keeps the root for the root output root" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val resolved =
                        GeneratedOutputPathResolver(fileSystem)
                            .resolveRoot(fileSystem.getPath("/out"), RelativePath.ROOT)

                    resolved shouldBe fileSystem.getPath("/out")
                }
            }

            "resolve rejects an output root that is a regular file" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val outputRoot = fileSystem.getPath("/out")
                    val space = DirectorySpace.from(outputRoot)
                    Files.delete(outputRoot)
                    Files.writeString(outputRoot, "not a directory")

                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputPathResolver(fileSystem).resolve(space, RelativePath.of("User.proto"))
                    }
                }
            }

            "resolve rejects a path that passes through a regular file" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    val space = DirectorySpace.from(fileSystem.getPath("/out"))
                    Files.writeString(fileSystem.getPath("/out/proto"), "not a directory")

                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputPathResolver(fileSystem).resolve(space, RelativePath.of("proto", "User.proto"))
                    }
                }
            }
        },
    )
