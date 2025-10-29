package me.liam.microsmith.gen.helpers

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.gen.core.GeneratorRegistry.getGenerator
import me.liam.microsmith.gen.files.FileSpace
import me.liam.microsmith.gen.files.TemporaryDirectory

suspend fun MicrosmithModel.generate(finalDir: FileSpace) = coroutineScope {
    TemporaryDirectory.create().use { tempSpace ->
        runGenerators(tempSpace)
    }

    runGenerators(finalDir)
    println("✅ Generated all files in ${finalDir.root}")
}

private suspend fun MicrosmithModel.runGenerators(
    tempSpace: FileSpace
) = coroutineScope {
    extensions().map { ext ->
        async {
            val gen = ext.getGenerator()
            if (gen == null) {
                println("⚠️ No generator found for ${ext::class.simpleName}")
                return@async emptyList()
            }
            gen.run { ext.generate(tempSpace) }.also {
                println("🛠️ Generated ${it.size} files for ${ext::class.simpleName} in ${tempSpace.root}")
            }
        }
    }
        .awaitAll()
        .flatten()
}
