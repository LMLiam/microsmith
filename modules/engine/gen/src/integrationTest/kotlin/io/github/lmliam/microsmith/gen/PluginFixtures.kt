package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import thirdparty.contract.FixtureExtension

object PluginFixtures {
    private val directory: Path =
        Path.of(
            checkNotNull(System.getProperty("microsmith.fixturePlugins")) { "microsmith.fixturePlugins is not set" },
        )

    fun classLoader(
        vararg names: String,
        parent: ClassLoader = PluginFixtures::class.java.classLoader,
    ): URLClassLoader =
        URLClassLoader(names.map { directory.resolve("$it.jar").toUri().toURL() }.toTypedArray(), parent)
}

fun fixtureModel(text: String = "hello"): MicrosmithModel =
    MicrosmithBuilder().apply { put(FixtureExtension::class, FixtureExtension(text)) }.model

fun newOutputDirectory(): Path = createTempDirectory("microsmith-plugin-run-")

suspend fun generateWithPlugins(
    vararg plugins: String,
    diagnostics: DiagnosticSink = RecordingDiagnosticSink(),
    output: Path = newOutputDirectory(),
    parent: ClassLoader = PluginFixtures::class.java.classLoader,
): Path {
    testGenerator(diagnostics).generateTo(fixtureModel(), output, PluginFixtures.classLoader(*plugins, parent = parent))

    return output
}

@OptIn(ExperimentalPathApi::class)
fun generatedTexts(directory: Path): Map<String, String> =
    directory.walk().filter { it.isRegularFile() }.associate { it.relativeTo(directory).toString() to it.readText() }
