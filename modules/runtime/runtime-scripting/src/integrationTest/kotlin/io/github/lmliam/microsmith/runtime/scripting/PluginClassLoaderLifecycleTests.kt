package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.runtime.scripting.host.execution.PLUGIN_THREAD_LEFT_RUNNING_CODE
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import java.lang.ref.WeakReference
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import thirdparty.contract.CollectabilityProbe
import thirdparty.contract.STUBBORN_THREAD_NAME

class PluginClassLoaderLifecycleTests :
    StringSpec(
        {
            beforeTest { CollectabilityProbe.reset() }

            "the plugin class loader, a plugin class and a plugin instance are collected after a script run" {
                inTempDirectory { tempDir ->
                    val diagnostics = RecordingDiagnosticSink()
                    val request = tempDir.request("collectability", "leakscripting", "leak")

                    testScriptHost(diagnostics = diagnostics, cacheDirectory = tempDir.resolve("cache"))
                        .run(request)
                        .shouldBeTypeOf<ScriptRunSuccess>()

                    CollectabilityProbe.loaders.shouldNotBeEmpty()
                    CollectabilityProbe.classes.shouldNotBeEmpty()
                    CollectabilityProbe.instances.shouldNotBeEmpty()
                    collectGarbage(trackedReferences()) shouldBe true
                    diagnostics.diagnostics.filter { it.code == PLUGIN_THREAD_LEFT_RUNNING_CODE }.shouldBeEmpty()
                }
            }

            "a thread local a plugin set during the script does not keep its loader" {
                inTempDirectory { tempDir ->
                    val request = tempDir.request("scripting-plugin-symbols", "threadlocalscripting")

                    testScriptHost(cacheDirectory = tempDir.resolve("cache"))
                        .run(request)
                        .shouldBeTypeOf<ScriptRunSuccess>()

                    CollectabilityProbe.loaders.shouldNotBeEmpty()
                    collectGarbage(trackedReferences()) shouldBe true
                }
            }

            "repeated runs with different plugin class paths never share classes or change the context class loader" {
                inTempDirectory { tempDir ->
                    val caller = Thread.currentThread()
                    val contextClassLoader = caller.contextClassLoader
                    val host = testScriptHost(cacheDirectory = tempDir.resolve("cache"))

                    host
                        .run(tempDir.request("scripting-plugin-symbols", "symbolsscripting"))
                        .shouldBeTypeOf<ScriptRunSuccess>()
                    caller.contextClassLoader shouldBe contextClassLoader

                    host.run(tempDir.request("scripting-plugin-symbols")).shouldBeTypeOf<ScriptRunFailure>()
                    caller.contextClassLoader shouldBe contextClassLoader
                }
            }

            "a plugin thread that ignores interruption keeps its loader reachable and is reported" {
                inTempDirectory { tempDir ->
                    val diagnostics = RecordingDiagnosticSink()
                    val request = tempDir.request("scripting-plugin-symbols", "stubbornthreadscripting")

                    try {
                        testScriptHost(diagnostics = diagnostics, cacheDirectory = tempDir.resolve("cache"))
                            .run(request)
                            .shouldBeTypeOf<ScriptRunSuccess>()

                        CollectabilityProbe.loaders.shouldNotBeEmpty()
                        collectGarbage(trackedReferences()) shouldBe false
                        CollectabilityProbe.loaders.forEach { it.get() shouldNotBe null }
                        val warnings = diagnostics.diagnostics.filter { it.code == PLUGIN_THREAD_LEFT_RUNNING_CODE }
                        warnings shouldHaveSize 1
                        warnings.single().severity shouldBe DiagnosticSeverity.WARNING
                        warnings.single().message shouldContain FixturePlugins.jar("stubbornthreadscripting").toString()
                    } finally {
                        stopStubbornThread()
                    }
                }
            }

            "a script run after a leaked one still succeeds" {
                inTempDirectory { tempDir ->
                    val host = testScriptHost(cacheDirectory = tempDir.resolve("cache"))

                    try {
                        host
                            .run(tempDir.request("scripting-plugin-symbols", "stubbornthreadscripting"))
                            .shouldBeTypeOf<ScriptRunSuccess>()
                        host.run(tempDir.request("generate-three-times", "counting")).shouldBeTypeOf<ScriptRunSuccess>()
                    } finally {
                        stopStubbornThread()
                    }
                }
            }
        },
    )

@OptIn(ExperimentalPathApi::class)
private suspend fun inTempDirectory(block: suspend (Path) -> Unit) {
    val tempDir = createTempDirectory("microsmith-plugin-class-loader-lifecycle")
    try {
        block(tempDir)
    } finally {
        runCatching { tempDir.deleteRecursively() }
    }
}

private fun Path.request(scriptResource: String, vararg pluginJars: String): ScriptRunRequest {
    val script = resolve("$scriptResource.microsmith.kts")
    copyResource("$scriptResource.microsmith.kts", script)

    return ScriptRunRequest(
        script = script,
        outputDir = resolve("generated"),
        variables = emptyMap(),
        flags = emptySet(),
        pluginClasspath = pluginJars.map(FixturePlugins::jar),
    )
}

private fun trackedReferences(): List<WeakReference<*>> =
    CollectabilityProbe.loaders + CollectabilityProbe.classes + CollectabilityProbe.instances

private suspend fun collectGarbage(references: List<WeakReference<*>>): Boolean {
    repeat(GC_ROUNDS) { _ ->
        delay(GC_PAUSE)
        System.gc()
        if (references.all { it.get() == null }) return true
    }
    return false
}

private suspend fun stopStubbornThread() {
    CollectabilityProbe.stopRequested = true
    repeat(GC_ROUNDS) { _ ->
        if (Thread.getAllStackTraces().keys.none { it.name == STUBBORN_THREAD_NAME && it.isAlive }) return
        delay(GC_PAUSE)
    }
}

private const val GC_ROUNDS = 60
private val GC_PAUSE = 50.milliseconds
