package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScriptCompilationConfiguration
import io.github.lmliam.microsmith.runtime.scripting.definition.MicrosmithScriptEvaluationConfiguration
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

@OptIn(ExperimentalPathApi::class)
class ScriptingPluginSymbolsTests :
    StringSpec(
        {
            "a scripting plugin in a built jar contributes symbols to a script" {
                val tempDir = createTempDirectory("microsmith-scripting-plugin-symbols")
                try {
                    val script = tempDir.resolve("symbols.microsmith.kts")
                    copyResource("scripting-plugin-symbols.microsmith.kts", script)

                    testScriptHost(cacheDirectory = tempDir.resolve("cache"))
                        .run(
                            ScriptRunRequest(
                                script = script,
                                outputDir = tempDir.resolve("generated"),
                                variables = emptyMap(),
                                flags = emptySet(),
                                pluginClasspath = listOf(FixturePlugins.jar("symbolsscripting")),
                            ),
                        )
                        .shouldBeTypeOf<ScriptRunSuccess>()
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }

            "the script definition discovers built-in symbols when the host sets no registry" {
                val tempDir = createTempDirectory("microsmith-script-definition-symbols")
                try {
                    val script = tempDir.resolve("symbols.microsmith.kts")
                    copyResource("forward-symbols.microsmith.kts", script)
                    val context = MicrosmithScriptContext(tempDir, emptyMap(), emptySet()) { emptyList() }

                    BasicJvmScriptingHost()
                        .eval(
                            FileScriptSource(script.toFile()),
                            MicrosmithScriptCompilationConfiguration,
                            ScriptEvaluationConfiguration(MicrosmithScriptEvaluationConfiguration) {
                                implicitReceivers(context)
                            },
                        )
                        .shouldBeInstanceOf<ResultWithDiagnostics.Success<*>>()
                } finally {
                    runCatching { tempDir.deleteRecursively() }
                }
            }
        },
    )
