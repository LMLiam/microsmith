package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.dsl.microsmith
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists

class MicrosmithModelGenerationExtensionsTests :
    StringSpec(
        {
            "generateTo creates output directory when it does not exist" {
                val workspaceRoot = createTempDirectory("microsmith-generate-to-root-")
                val outputDir = workspaceRoot.resolve("generated/proto")
                val model = microsmith {}

                model.generateTo(outputDir)

                outputDir.exists() shouldBe true
            }
        },
    )
