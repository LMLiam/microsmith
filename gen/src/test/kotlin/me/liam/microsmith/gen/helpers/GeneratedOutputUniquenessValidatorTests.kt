package me.liam.microsmith.gen.helpers

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import me.liam.microsmith.gen.files.to
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
