package me.liam.microsmith.maven

import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

internal class MicrosmithMavenExecutionRequestFactory {
    fun create(configuration: MicrosmithMavenExecutionConfiguration): MicrosmithMavenExecutionRequest {
        val baseDirectory = configuration.baseDirectory.toAbsolutePath().normalize()
        val scriptPath = resolveAgainstBaseDirectory(baseDirectory, configuration.scriptFile)
        val outputPath = resolveAgainstBaseDirectory(baseDirectory, configuration.outputDirectory)
        val cachePath = resolveAgainstBaseDirectory(baseDirectory, configuration.cacheDirectory)
        return MicrosmithMavenExecutionRequest(
            scriptRunRequest =
            ScriptRunRequest(
                script = scriptPath,
                outputDir = outputPath,
                variables = configuration.variables.toSortedStringMap(),
                flags = configuration.flags.toNormalizedFlagSet(),
            ),
            outputDirectory = outputPath,
            cacheDirectory = cachePath,
        )
    }

    private fun resolveAgainstBaseDirectory(baseDirectory: Path, path: Path): Path =
        path.takeIf(Path::isAbsolute)?.normalize() ?: baseDirectory.resolve(path).normalize()
}

private fun java.util.Properties?.toSortedStringMap(): Map<String, String> = this?.stringPropertyNames()
    ?.sorted()
    ?.associateWith { propertyName -> getProperty(propertyName) }
    .orEmpty()

private fun List<String>?.toNormalizedFlagSet(): Set<String> = this.orEmpty()
    .asSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .toSortedSet()
