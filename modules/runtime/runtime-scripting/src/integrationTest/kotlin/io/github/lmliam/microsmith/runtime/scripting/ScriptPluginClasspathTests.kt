package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeTypeOf
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
class ScriptPluginClasspathTests :
    StringSpec(
        {
            "runs a script when the plugin class path is not empty" {
                val tempDir = createTempDirectory("microsmith-script-plugin-classpath")
                try {
                    val script = tempDir.resolve("schema.microsmith.kts")
                    copyResource("protobuf-user.microsmith.kts", script)

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
        },
    )
