package io.github.lmliam.microsmith.runtime.scripting.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath

@OptIn(ExperimentalPathApi::class)
class MicrosmithScriptCacheTests :
    StringSpec({
        "classpath fingerprint changes when classpath file content changes" {
            val tempDir = createTempDirectory("microsmith-classpath-fingerprint")
            try {
                val classpathEntry = tempDir.resolve("cli.jar")
                classpathEntry.writeText("abc")

                val firstFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))

                classpathEntry.writeText("xyz")
                val secondFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))

                secondFingerprint shouldNotBe firstFingerprint
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "classpath fingerprint is stable when classpath file content is unchanged" {
            val tempDir = createTempDirectory("microsmith-classpath-fingerprint-stable")
            try {
                val classpathEntry = tempDir.resolve("cli.jar")
                classpathEntry.writeText("same-content")

                val firstFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))
                val secondFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))

                secondFingerprint shouldBe firstFingerprint
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "compiled script fingerprint changes when plugin classpath bytes change at same path" {
            val tempDir = createTempDirectory("microsmith-compiled-fingerprint-plugin-change")
            try {
                val scriptFile = tempDir.resolve("schema.microsmith.kts")
                val pluginJar = tempDir.resolve("plugins/custom-plugin.jar")
                scriptFile.writeText("microsmith { }")
                pluginJar.parent.createDirectories()
                pluginJar.writeText("v1")

                val compilationConfiguration =
                    ScriptCompilationConfiguration {
                        jvm {
                            updateClasspath(listOf(pluginJar.toFile()))
                        }
                    }
                val scriptSource = FileScriptSource(scriptFile.toFile())

                val firstFingerprint =
                    CompiledScriptFingerprint.uniqueName(
                        script = scriptSource,
                        scriptCompilationConfiguration = compilationConfiguration,
                        additionalFingerprints = listOf(RuntimeClasspathFingerprint.calculate(listOf(pluginJar))),
                    )

                pluginJar.writeText("v2")

                val secondFingerprint =
                    CompiledScriptFingerprint.uniqueName(
                        script = scriptSource,
                        scriptCompilationConfiguration = compilationConfiguration,
                        additionalFingerprints = listOf(RuntimeClasspathFingerprint.calculate(listOf(pluginJar))),
                    )

                secondFingerprint shouldNotBe firstFingerprint
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
