package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

fun newOutputDirectory(): Path = Files.createTempDirectory("microsmith-plugin-run-")

suspend fun generateWithPlugins(
    vararg plugins: String,
    diagnostics: DiagnosticSink = RecordingDiagnosticSink(),
    output: Path = newOutputDirectory(),
    parent: ClassLoader = PluginFixtures::class.java.classLoader,
): Path {
    testGenerator(diagnostics).generateTo(fixtureModel(), output, PluginFixtures.classLoader(*plugins, parent = parent))

    return output
}

suspend fun runCancelled(
    vararg plugins: String,
    trigger: CancelTrigger,
    output: Path,
    temporaryRoot: Path,
    dispatcher: CoroutineDispatcher,
): CancelledRun {
    val diagnostics = RecordingDiagnosticSink()
    lateinit var run: Job
    val sink =
        object : DiagnosticSink {
            override fun report(diagnostic: Diagnostic) {
                diagnostics.report(diagnostic)
                if (diagnostic.code == trigger.diagnosticCode) run.cancel()
            }
        }
    val environment =
        IsolatedTemporaryEnvironment(temporaryRoot) {
            if (trigger == CancelTrigger.TEMPORARY_DIRECTORY_REQUESTED) run.cancel()
        }

    coroutineScope {
        run =
            launch(start = CoroutineStart.LAZY) {
                testGenerator(sink, dispatcher, environment)
                    .generateTo(fixtureModel(), output, PluginFixtures.classLoader(*plugins))
            }
        run.start()
        run.join()
    }

    check(run.isCancelled) { "the run was not cancelled" }

    return CancelledRun(temporaryRoot, diagnostics)
}

@OptIn(ExperimentalPathApi::class)
fun generatedTexts(directory: Path): Map<String, String> =
    directory.walk().filter { it.isRegularFile() }.associate { it.relativeTo(directory).toString() to it.readText() }
