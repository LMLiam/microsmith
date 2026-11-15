package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.files.FileSpace
import java.nio.file.Path

public interface MicrosmithGenerator {
    public suspend fun generate(model: MicrosmithModel, finalDir: FileSpace, pluginClassLoader: ClassLoader): List<Path>

    public suspend fun generateTo(model: MicrosmithModel, outputDir: Path, pluginClassLoader: ClassLoader): List<Path>
}
