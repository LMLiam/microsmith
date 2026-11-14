package io.github.lmliam.microsmith.artifact.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

class FileArtifactIdTests :
    StringSpec(
        {
            "a text file id rejects the root as its path" {
                shouldThrow<IllegalArgumentException> { TextFileArtifactId(RelativePath.ROOT) }
            }

            "a binary file id rejects the root as its path" {
                shouldThrow<IllegalArgumentException> { BinaryFileArtifactId(RelativePath.ROOT) }
            }

            "a generated file rejects the root as its path" {
                shouldThrow<IllegalArgumentException> { GeneratedFile(RelativePath.ROOT, byteArrayOf()) }
            }
        },
    )
