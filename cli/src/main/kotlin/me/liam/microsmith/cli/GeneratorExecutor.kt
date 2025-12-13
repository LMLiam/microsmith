package me.liam.microsmith.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.runBlocking
import me.liam.microsmith.dsl.core.MicrosmithExtension
import me.liam.microsmith.dsl.core.MicrosmithModel
import me.liam.microsmith.gen.core.GenerationContext
import me.liam.microsmith.gen.core.MicrosmithGenerator
import me.liam.microsmith.gen.core.ModelGenerator
import me.liam.microsmith.gen.files.GeneratedFile
import java.util.ServiceLoader
import kotlin.reflect.KClass

data class GeneratorRun(
    val id: String,
    val files: List<Path>
)

class GenerationFailure(
    val generatorId: String,
    cause: Throwable
) : RuntimeException(cause)

class GeneratorExecutor(
    private val classLoader: ClassLoader,
    private val log: (String) -> Unit = {}
) {
    fun runGenerators(
        model: MicrosmithModel,
        outputDir: Path,
        requested: Set<String>?
    ): List<GeneratorRun> {
        val generators = loadGenerators()
        val selected =
            generators.filter { candidate ->
                requested.isNullOrEmpty() || requested.contains(candidate.id)
            }

        if (!requested.isNullOrEmpty() && selected.isEmpty()) {
            throw IllegalArgumentException("No generators matched filters: ${requested.joinToString()}")
        }

        val context = GenerationContext(outputDir, log)
        return selected.map { generator ->
            val generatedFiles =
                try {
                    generator.run(model, context)
                } catch (ex: Exception) {
                    throw GenerationFailure(generator.id, ex)
                }

            val written = writeOutputs(outputDir, generatedFiles)
            log("Ran ${generator.id} -> ${written.size} file(s)")
            GeneratorRun(generator.id, written)
        }
    }

    private fun loadGenerators(): List<LoadedGenerator> {
        val explicit =
            ServiceLoader.load(MicrosmithGenerator::class.java, classLoader)
                .map { gen ->
                    LoadedGenerator(gen.id.lowercase()) { model, ctx ->
                        gen.generate(model, ctx)
                    }
                }

        val adapterBacked =
            ServiceLoader.load(ModelGenerator::class.java, classLoader)
                .map { adapt(it) }

        return (explicit + adapterBacked).associateBy { it.id }.values.toList()
    }

    private fun writeOutputs(
        outputDir: Path,
        files: List<GeneratedFile>
    ): List<Path> =
        files.map { generated ->
            val target = outputDir.resolve(generated.relativePath)
            target.parent?.let { Files.createDirectories(it) }
            Files.write(target, generated.contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        }

    @Suppress("UNCHECKED_CAST")
    private fun <T : MicrosmithExtension> adapt(generator: ModelGenerator<T>): LoadedGenerator {
        val derivedId =
            when (generator) {
                is MicrosmithGenerator -> generator.id
                else -> generator.extension.simpleName ?: generator::class.simpleName ?: "generator"
            }.lowercase()

        val extensionType = generator.extension as KClass<T>
        return LoadedGenerator(derivedId) { model, ctx ->
            val ext = model.get(extensionType) ?: return@LoadedGenerator emptyList()
            runBlocking { generator.run { ext.generate(ctx) } }
        }
    }

    private data class LoadedGenerator(
        val id: String,
        val run: (MicrosmithModel, GenerationContext) -> List<GeneratedFile>
    )
}
