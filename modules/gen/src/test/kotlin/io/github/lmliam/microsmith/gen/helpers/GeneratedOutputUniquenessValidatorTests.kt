package io.github.lmliam.microsmith.gen.helpers

import io.github.lmliam.microsmith.gen.files.to
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import kotlin.io.path.Path

class GeneratedOutputUniquenessValidatorTests :
    StringSpec({
        "requireUniqueRelativePaths rejects duplicate normalized output paths" {
            shouldThrow<IllegalArgumentException> {
                GeneratedOutputUniquenessValidator.requireUniqueRelativePaths(
                    listOf(
                        Path("proto/User.proto") to byteArrayOf(1),
                        Path("proto/./User.proto") to byteArrayOf(2),
                    ),
                )
            }
        }
    })
