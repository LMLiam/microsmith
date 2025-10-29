package me.liam.microsmith.gen.helpers

import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.gen.core.GeneratorRegistry.getGenerator

suspend fun MicrosmithModel.generate() {
    extensions().forEach { ext ->
        ext.getGenerator()
            .runCatching { ext.generate() }
            .onSuccess { println("Generated ${ext::class.simpleName}") }
            .onFailure { error("Error generating extension ${ext::class.simpleName}: ${it.message}") }
    }
}