package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import thirdparty.contract.CONTRIBUTING_CODE
import thirdparty.contract.RESOLVING_CODE

@OptIn(ExperimentalPathApi::class)
class ScriptRunCancellationTests :
    StringSpec(
        {
            "cancelling a script run stops its in-flight generation" {
                val tempDir = createTempDirectory("microsmith-script-run-cancellation")
                try {
                    val script = tempDir.resolve("script.microsmith.kts")
                    copyResource("generate-fixture-extension.microsmith.kts", script)
                    val output = tempDir.resolve("generated").createDirectories()
                    val diagnostics = RecordingDiagnosticSink()
                    lateinit var run: Job
                    val sink =
                        object : DiagnosticSink {
                            override fun report(diagnostic: Diagnostic) {
                                diagnostics.report(diagnostic)
                                if (diagnostic.code == RESOLVING_CODE) run.cancel()
                            }
                        }
                    val host = testScriptHost(diagnostics = sink, cacheDirectory = tempDir.resolve("cache"))

                    coroutineScope {
                        run =
                            launch(start = CoroutineStart.LAZY) {
                                host.run(
                                    ScriptRunRequest(
                                        script = script,
                                        outputDir = output,
                                        variables = emptyMap(),
                                        flags = emptySet(),
                                        pluginClasspath = listOf(FixturePlugins.jar("stalling")),
                                    ),
                                )
                            }
                        run.start()
                        withTimeout(1.minutes) { run.join() }
                    }

                    run.isCancelled shouldBe true
                    diagnostics.diagnostics.map { it.code } shouldContain RESOLVING_CODE
                    diagnostics.diagnostics.map { it.code } shouldNotContain CONTRIBUTING_CODE
                    output.listDirectoryEntries().shouldBeEmpty()
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }
        },
    )
