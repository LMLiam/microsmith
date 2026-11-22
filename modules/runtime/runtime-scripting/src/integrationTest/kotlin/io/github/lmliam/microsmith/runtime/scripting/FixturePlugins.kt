package io.github.lmliam.microsmith.runtime.scripting

import java.nio.file.Path
import kotlin.io.path.Path

object FixturePlugins {
    private val directory: Path =
        Path(checkNotNull(System.getProperty("microsmith.fixturePlugins")) { "microsmith.fixturePlugins is not set" })

    fun jar(name: String): Path = directory.resolve("$name.jar")
}
