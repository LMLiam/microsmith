package io.github.lmliam.microsmith.sbt

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import java.nio.file.Path

class MicrosmithSbtExecutionRequestFactory {
    fun create(configuration: MicrosmithSbtExecutionConfiguration): MicrosmithSbtExecutionRequest {
        val baseDirectory = configuration.baseDirectory.toAbsolutePath().normalize()
        val scriptPath = resolveAgainstBaseDirectory(baseDirectory, configuration.scriptFile)
        val outputPath = resolveAgainstBaseDirectory(baseDirectory, configuration.outputDirectory)
        val cachePath = resolveAgainstBaseDirectory(baseDirectory, configuration.cacheDirectory)
        return MicrosmithSbtExecutionRequest(
            scriptRunRequest =
            ScriptRunRequest(
                script = scriptPath,
                outputDir = outputPath,
                variables = configuration.variables.toSortedMap(),
                flags = configuration.flags.toSortedSet(),
            ),
            outputDirectory = outputPath,
            cacheDirectory = cachePath,
        )
    }

    private fun resolveAgainstBaseDirectory(baseDirectory: Path, path: Path): Path =
        path.takeIf(Path::isAbsolute)?.normalize() ?: baseDirectory.resolve(path).normalize()
}
