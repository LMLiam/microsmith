package me.liam.microsmith.cli.test

import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.gen.core.GenerationContext
import me.liam.microsmith.gen.core.MicrosmithGenerator
import me.liam.microsmith.gen.files.GeneratedFile

class FailingGenerator : MicrosmithGenerator {
    override val id = "failing"

    override fun generate(
        model: MicrosmithModel,
        context: GenerationContext
    ): List<GeneratedFile> = error("intentional failure for tests")
}
