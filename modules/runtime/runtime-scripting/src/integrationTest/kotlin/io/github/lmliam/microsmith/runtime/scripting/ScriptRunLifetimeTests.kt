package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.time.Duration.Companion.milliseconds
import thirdparty.contract.GENERATION_RUN_OPENED_CODE

@OptIn(ExperimentalPathApi::class)
class ScriptRunLifetimeTests :
    StringSpec(
        {
            "a script that calls generate three times runs three generations in one script run and compiles once" {
                val tempDir = createTempDirectory("microsmith-script-run-lifetime")
                try {
                    val cache = tempDir.resolve("cache")
                    val diagnostics = RecordingDiagnosticSink()
                    val host = testScriptHost(diagnostics = diagnostics, cacheDirectory = cache)
                    val request = tempDir.generateThreeTimesRequest()

                    host.run(request).shouldBeTypeOf<ScriptRunSuccess>().cacheHit shouldBe false

                    diagnostics.diagnostics.filter { it.code == GENERATION_RUN_OPENED_CODE } shouldHaveSize 3
                    cache.listDirectoryEntries("*.jar") shouldHaveSize 1
                    host.run(request).shouldBeTypeOf<ScriptRunSuccess>().cacheHit shouldBe true
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "the reported elapsed time comes from the injected time source" {
                val tempDir = createTempDirectory("microsmith-script-run-time-source")
                try {
                    val step = 250.milliseconds
                    val host =
                        testScriptHost(
                            timeSource = SteppingTimeSource(step),
                            cacheDirectory = tempDir.resolve("cache"),
                        )

                    host
                        .run(tempDir.generateThreeTimesRequest())
                        .shouldBeTypeOf<ScriptRunSuccess>()
                        .elapsedMillis shouldBe step.inWholeMilliseconds
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }
        },
    )

private fun Path.generateThreeTimesRequest(): ScriptRunRequest {
    val script = resolve("script.microsmith.kts")
    copyResource("generate-three-times.microsmith.kts", script)

    return ScriptRunRequest(
        script = script,
        outputDir = resolve("generated"),
        variables = emptyMap(),
        flags = emptySet(),
        pluginClasspath = listOf(FixturePlugins.jar("counting")),
    )
}
