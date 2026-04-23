package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import java.nio.file.Files
import kotlin.io.path.Path

class GeneratedOutputUniquenessValidatorTests :
    StringSpec({
        "requireUniqueOutputPaths rejects absolute output roots" {
            shouldThrow<IllegalArgumentException> {
                GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(
                    listOf(
                        GeneratedFile(
                            relativePath = Path("User.proto"),
                            contents = byteArrayOf(1),
                            outputRoot = Path("/tmp/proto"),
                        ),
                    ),
                )
            }
        }

        "requireUniqueOutputPaths rejects output roots that traverse outside the root" {
            shouldThrow<IllegalArgumentException> {
                GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(
                    listOf(
                        GeneratedFile(
                            relativePath = Path("User.proto"),
                            contents = byteArrayOf(1),
                            outputRoot = Path("../proto"),
                        ),
                    ),
                )
            }
        }

        "requireUniqueOutputPaths rejects duplicate normalized output paths" {
            shouldThrow<IllegalArgumentException> {
                GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(
                    listOf(
                        GeneratedFile(
                            relativePath = Path("User.proto"),
                            contents = byteArrayOf(1),
                            outputRoot = Path("proto"),
                        ),
                        GeneratedFile(
                            relativePath = Path("./User.proto"),
                            contents = byteArrayOf(2),
                            outputRoot = Path("proto"),
                        ),
                    ),
                )
            }
        }

        "requireUniqueOutputPaths rejects absolute generated file paths" {
            val absolutePath = Files.createTempDirectory("microsmith-uniqueness-")
                .resolve("User.proto")
                .toAbsolutePath()

            shouldThrow<IllegalArgumentException> {
                GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(
                    listOf(
                        GeneratedFile(
                            relativePath = absolutePath,
                            contents = byteArrayOf(1),
                            outputRoot = Path("proto"),
                        ),
                    ),
                )
            }
        }

        "requireUniqueOutputPaths rejects generated file paths that traverse outside the root" {
            shouldThrow<IllegalArgumentException> {
                GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(
                    listOf(
                        GeneratedFile(
                            relativePath = Path("../outside.proto"),
                            contents = byteArrayOf(1),
                            outputRoot = Path("proto"),
                        ),
                    ),
                )
            }
        }

        "requireUniqueOutputPaths allows the same relative file under distinct output roots" {
            GeneratedOutputUniquenessValidator.requireUniqueOutputPaths(
                listOf(
                    GeneratedFile(
                        relativePath = Path("User.proto"),
                        contents = byteArrayOf(1),
                        outputRoot = Path("proto"),
                    ),
                    GeneratedFile(
                        relativePath = Path("User.proto"),
                        contents = byteArrayOf(2),
                        outputRoot = Path("services/UserService"),
                    ),
                ),
            )
        }
    })
