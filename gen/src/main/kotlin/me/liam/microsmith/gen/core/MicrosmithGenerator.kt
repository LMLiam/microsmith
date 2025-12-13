package me.liam.microsmith.gen.core

import com.github.eventhorizonlab.spi.ServiceContract
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.GeneratedFile
import java.nio.file.Path

data class GenerationContext(
    override val root: Path,
    val log: (String) -> Unit = {}
) : FileSpace

@ServiceContract
interface MicrosmithGenerator {
    val id: String

    fun generate(model: MicrosmithModel, context: GenerationContext): List<GeneratedFile>
}
