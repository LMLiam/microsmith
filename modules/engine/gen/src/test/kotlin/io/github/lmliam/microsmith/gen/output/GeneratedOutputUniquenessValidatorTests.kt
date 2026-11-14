package io.github.lmliam.microsmith.gen.output

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import io.github.lmliam.microsmith.artifact.files.GeneratedFile
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

private fun generated(relativePath: String, outputRoot: String = ".", contents: Byte = 1): GeneratedFile =
    GeneratedFile(
        relativePath = RelativePath.of(relativePath),
        contents = byteArrayOf(contents),
        outputRoot = RelativePath.of(outputRoot),
    )

class GeneratedOutputUniquenessValidatorTests :
    StringSpec(
        {
            "requireUniqueOutputPaths rejects duplicate normalized output paths" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputUniquenessValidator(fileSystem)
                            .requireUniqueOutputPaths(
                                listOf(
                                    generated("User.proto", outputRoot = "proto"),
                                    generated("./User.proto", outputRoot = "proto", contents = 2),
                                ),
                            )
                    }
                }
            }

            "requireUniqueOutputPaths rejects a file that reaches the same path through a different output root" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputUniquenessValidator(fileSystem)
                            .requireUniqueOutputPaths(
                                listOf(
                                    generated("User.proto", outputRoot = "proto"),
                                    generated("proto/User.proto", contents = 2),
                                ),
                            )
                    }
                }
            }

            "requireUniqueOutputPaths allows the same relative file under distinct output roots" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    GeneratedOutputUniquenessValidator(fileSystem)
                        .requireUniqueOutputPaths(
                            listOf(
                                generated("User.proto", outputRoot = "proto"),
                                generated("User.proto", outputRoot = "services/UserService", contents = 2),
                            ),
                        )
                }
            }

            "requireUniqueOutputPaths rejects names that differ only in case on a case-insensitive file system" {
                Jimfs.newFileSystem(Configuration.windows()).use { fileSystem ->
                    shouldThrow<IllegalArgumentException> {
                        GeneratedOutputUniquenessValidator(fileSystem)
                            .requireUniqueOutputPaths(
                                listOf(generated("User.proto"), generated("user.proto", contents = 2)),
                            )
                    }
                }
            }

            "requireUniqueOutputPaths allows names that differ only in case on a case-sensitive file system" {
                Jimfs.newFileSystem(Configuration.unix()).use { fileSystem ->
                    GeneratedOutputUniquenessValidator(fileSystem)
                        .requireUniqueOutputPaths(
                            listOf(generated("User.proto"), generated("user.proto", contents = 2)),
                        )
                }
            }
        },
    )
