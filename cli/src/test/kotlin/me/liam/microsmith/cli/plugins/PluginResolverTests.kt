package me.liam.microsmith.cli.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import me.liam.microsmith.cli.command.RunCommand
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverTests :
    StringSpec({
        "resolves plugin coordinate from configured repository and writes lockfile" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:microsmith-emitter-ts:1.4.2"
                val repositoryJar =
                    repositoryRoot.resolve(
                        "com/acme/microsmith-emitter-ts/1.4.2/microsmith-emitter-ts-1.4.2.jar"
                    )
                repositoryJar.parent?.toFile()?.mkdirs()
                repositoryJar.writeBytes("plugin-jar-contents".toByteArray())
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        plugins = setOf(coordinate),
                        repositoryOverride = repositoryRoot.toUri().toString()
                    )
                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = cache)
                    )

                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath.shouldHaveSize(1)
                success.classpath.first().exists() shouldBe true
                val lockfilePath = requireNotNull(success.lockfilePath)
                lockfilePath.exists() shouldBe true
                lockfilePath.readLines().joinToString("\n").shouldContain("version=1")
                lockfilePath.readLines().joinToString("\n").shouldContain("remote|$coordinate|")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails with remediation message when offline plugin artifact is missing from cache" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-offline")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:missing:1.0.0"),
                        offline = true
                    )

                val result =
                    resolvePlugins(
                        command = command,
                        settings = PluginResolverSettings(cacheDirectory = tempDir.resolve("cache"))
                    )

                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldContain("Offline mode is enabled")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "fails when cached artifact checksum does not match existing lockfile" {
            val tempDir = createTempDirectory("microsmith-plugin-resolver-lock-mismatch")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                val output = tempDir.resolve("generated")
                val cache = tempDir.resolve("cache")
                val repositoryRoot = tempDir.resolve("repo")
                val coordinate = "com.acme:lock-test:1.0.0"
                val repositoryJar =
                    repositoryRoot.resolve("com/acme/lock-test/1.0.0/lock-test-1.0.0.jar")
                repositoryJar.parent?.toFile()?.mkdirs()
                repositoryJar.writeBytes("initial".toByteArray())
                script.writeText("// test script")

                val command =
                    RunCommand(
                        script = script,
                        outputDir = output,
                        plugins = setOf(coordinate),
                        repositoryOverride = repositoryRoot.toUri().toString()
                    )

                resolvePlugins(command = command, settings = PluginResolverSettings(cacheDirectory = cache))

                val cachedArtifact =
                    cache.resolve("artifacts/com/acme/lock-test/1.0.0/lock-test-1.0.0.jar")
                cachedArtifact.writeBytes("tampered".toByteArray())

                val mismatch =
                    resolvePlugins(command = command, settings = PluginResolverSettings(cacheDirectory = cache))
                        .shouldBeTypeOf<PluginResolutionResult.Failure>()

                mismatch.diagnostics.joinToString("\n").shouldContain("Checksum mismatch")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
