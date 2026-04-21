package io.github.lmliam.microsmith.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Path

@DisableCachingByDefault(
    because = "Microsmith maintains its own compilation cache and task-level caching needs deeper normalization.",
)
abstract class MicrosmithGenerateTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val scriptFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val variables: MapProperty<String, String>

    @get:Input
    abstract val flags: SetProperty<String>

    @get:Classpath
    abstract val pluginClasspath: ConfigurableFileCollection

    @get:Classpath
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:Internal
    abstract val cacheDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val request =
            MicrosmithGradleWorkerRequest(
                scriptPath = scriptFile.get().asFile.toPath(),
                outputPath = outputDirectory.get().asFile.toPath(),
                cacheDirectory = cacheDirectory.get().asFile.toPath(),
                variables = variables.get().toSortedMap(),
                flags = flags.get().toSortedSet(),
                pluginClasspath =
                pluginClasspath.files
                    .map { file -> file.toPath().toAbsolutePath().normalize() }
                    .sorted(),
            )
        val launcher = MicrosmithGradleWorkerLauncher()
        val result =
            launcher.execute(
                request = request,
                workDirectory = temporaryDir.toPath(),
                runtimeClasspath =
                runtimeClasspath.files
                    .map { file -> file.toPath().toAbsolutePath().normalize() }
                    .sorted(),
            )

        when (result) {
            is MicrosmithGradleWorkerSuccess -> reportSuccess(result)
            is MicrosmithGradleWorkerFailure -> throw GradleException(formatFailure(result))
        }
    }

    private fun reportSuccess(result: MicrosmithGradleWorkerSuccess) {
        result.warnings.forEach(logger::warn)
        val generatedOutputRoot =
            describeGeneratedOutputRoots(outputDirectory.get().asFile.toPath(), result.generatedRoots)
        logger.lifecycle(
            "Generated Microsmith outputs into '$generatedOutputRoot'. " +
                "(compile-cache=${if (result.cacheHit) "hit" else "miss"}, elapsed=${result.elapsedMillis}ms)",
        )
    }

    private fun formatFailure(result: MicrosmithGradleWorkerFailure): String = buildString {
        appendLine("Microsmith generation failed (${result.type.lowercase()}).")
        result.diagnostics.forEach(::appendLine)
    }.trimEnd()

    private fun describeGeneratedOutputRoots(outputDirectory: Path, roots: List<Path>): String {
        val normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize()
        val normalizedRoots = roots.map { root -> root.toAbsolutePath().normalize() }.distinct().sorted()
        return when (normalizedRoots.size) {
            0 -> normalizedOutputDirectory.toString()
            1 -> normalizedRoots.single().toString()
            else -> buildString {
                append(normalizedOutputDirectory)
                append(" (roots: ")
                append(normalizedRoots.joinToString())
                append(')')
            }
        }
    }
}
