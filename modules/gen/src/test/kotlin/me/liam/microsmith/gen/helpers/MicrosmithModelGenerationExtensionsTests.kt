package me.liam.microsmith.gen.helpers

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import me.liam.microsmith.dsl.core.microsmith
import java.nio.file.Files

class MicrosmithModelGenerationExtensionsTests :
    StringSpec({
        "generateTo creates output directory when it does not exist" {
            val workspaceRoot = Files.createTempDirectory("microsmith-generate-to-root-")
            val outputDir = workspaceRoot.resolve("generated/proto")
            val model = microsmith { }

            model.generateTo(outputDir)

            Files.exists(outputDir) shouldBe true
        }
    })
