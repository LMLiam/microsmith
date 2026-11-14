package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.dsl.MicrosmithBuilder
import io.github.lmliam.microsmith.gen.FirstLabelResolver
import io.github.lmliam.microsmith.gen.LabelExtension
import io.github.lmliam.microsmith.gen.LabelFileContributor
import io.github.lmliam.microsmith.gen.PluginFixtures
import io.github.lmliam.microsmith.gen.RecordingDiagnosticSink
import io.github.lmliam.microsmith.gen.ServiceProviderClassLoader
import io.github.lmliam.microsmith.gen.errors.PluginConflictException
import io.github.lmliam.microsmith.gen.errors.PluginLoadFailedException
import io.github.lmliam.microsmith.gen.generateWithPlugins
import io.github.lmliam.microsmith.gen.generatedTexts
import io.github.lmliam.microsmith.gen.newOutputDirectory
import io.github.lmliam.microsmith.gen.testGenerator
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.net.URLClassLoader
import java.nio.file.Files
import thirdparty.contract.FixtureExtension

private fun <T> List<T>.permutations(): List<List<T>> =
    if (size <= 1) listOf(this) else flatMap { head -> (this - head).permutations().map { listOf(head) + it } }

class PluginLoadingTests :
    StringSpec(
        {
            "two separately compiled plugins contribute to one generation run" {
                val output = generateWithPlugins("alpha", "beta")

                generatedTexts(output) shouldBe mapOf("alpha.txt" to "alpha:hello", "beta.txt" to "beta:hello")
            }

            "each plugin receives host services whose diagnostics carry its own id" {
                val diagnostics = RecordingDiagnosticSink()

                generateWithPlugins("alpha", "beta", diagnostics = diagnostics)

                diagnostics.closedPluginIds() shouldBe listOf("thirdparty.beta", "thirdparty.alpha")
            }

            "a conflicting contribution fails the run naming the artifact and every plugin is closed in reverse order" {
                val diagnostics = RecordingDiagnosticSink()

                val failure =
                    shouldThrow<IllegalArgumentException> {
                        generateWithPlugins("alpha", "beta", "conflicting", diagnostics = diagnostics)
                    }

                failure.message shouldContain "alpha.txt"
                diagnostics.closedPluginIds() shouldBe
                    listOf("thirdparty.conflicting", "thirdparty.beta", "thirdparty.alpha")
            }

            "a duplicate plugin id fails the run before any plugin is created" {
                val diagnostics = RecordingDiagnosticSink()
                val output = newOutputDirectory()

                val failure =
                    shouldThrow<PluginConflictException> {
                        generateWithPlugins("alpha", "duplicate", diagnostics = diagnostics, output = output)
                    }

                failure.message shouldContain "'thirdparty.alpha'"
                diagnostics.closedPluginIds() shouldBe emptyList()
                generatedTexts(output).shouldBeEmpty()
            }

            "a plugin whose services throws fails the run naming the plugin and releases the plugins before it" {
                val diagnostics = RecordingDiagnosticSink()
                val output = newOutputDirectory()

                val failure =
                    shouldThrow<PluginLoadFailedException> {
                        generateWithPlugins("alpha", "beta", "throwing", diagnostics = diagnostics, output = output)
                    }

                failure.message shouldContain "plugin 'thirdparty.throwing'"
                failure.message shouldContain "services failed on purpose"
                diagnostics.closedPluginIds() shouldBe listOf("thirdparty.beta", "thirdparty.alpha")
                generatedTexts(output).shouldBeEmpty()
            }

            "a plugin that cannot link against the host is reported as incompatible" {
                val failure = shouldThrow<PluginLoadFailedException> { generateWithPlugins("linkage") }

                failure.message shouldContain "plugin 'thirdparty.linkage'"
                failure.message shouldContain "incompatible"
                failure.cause?.shouldBeInstanceOf<LinkageError>()
            }

            "a provider class that does not exist fails the run naming the provider" {
                val services = Files.createTempDirectory("microsmith-broken-plugin-")
                val registration =
                    services.resolve("META-INF/services/io.github.lmliam.microsmith.plugin.MicrosmithPlugin")
                Files.createDirectories(registration.parent)
                Files.writeString(registration, "no.such.Plugin\n")

                val failure =
                    shouldThrow<PluginLoadFailedException> {
                        generateWithPlugins(
                            parent =
                                URLClassLoader(
                                    arrayOf(services.toUri().toURL()),
                                    PluginFixtures::class.java.classLoader,
                                ),
                        )
                    }

                failure.message shouldContain "no.such.Plugin"
            }

            "a plugin that fails to close is a warning and does not hide the pipeline failure" {
                val diagnostics = RecordingDiagnosticSink()

                val failure =
                    shouldThrow<IllegalArgumentException> {
                        generateWithPlugins("alpha", "closefailure", "conflicting", diagnostics = diagnostics)
                    }

                failure.message shouldContain "alpha.txt"
                failure.suppressed.map { it.message } shouldBe listOf("close failed on purpose")
                diagnostics.warnings().single().pluginId shouldBe "thirdparty.close-failure"
            }

            "a plugin that fails to close after a successful run is only a warning" {
                val diagnostics = RecordingDiagnosticSink()

                val output = generateWithPlugins("alpha", "closefailure", diagnostics = diagnostics)

                generatedTexts(output) shouldBe mapOf("alpha.txt" to "alpha:hello")
                diagnostics.warnings().single().pluginId shouldBe "thirdparty.close-failure"
            }

            "the output and the closing order do not depend on the discovery order" {
                val runs =
                    listOf("alpha", "beta", "closefailure").permutations().map { order ->
                        val diagnostics = RecordingDiagnosticSink()
                        val output = generateWithPlugins(*order.toTypedArray(), diagnostics = diagnostics)

                        generatedTexts(output) to diagnostics.closedPluginIds()
                    }

                runs.distinct() shouldHaveSize 1
                runs.first().second shouldBe listOf("thirdparty.beta", "thirdparty.alpha")
            }

            "repeated runs with different plugin class loaders never see each other's plugins" {
                val diagnostics = RecordingDiagnosticSink()

                val outputs =
                    listOf(listOf("alpha"), listOf("alpha", "beta"), listOf("alpha")).map { plugins ->
                        generatedTexts(generateWithPlugins(*plugins.toTypedArray(), diagnostics = diagnostics))
                    }

                outputs shouldBe
                    listOf(
                        mapOf("alpha.txt" to "alpha:hello"),
                        mapOf("alpha.txt" to "alpha:hello", "beta.txt" to "beta:hello"),
                        mapOf("alpha.txt" to "alpha:hello"),
                    )
                diagnostics.closedPluginIds() shouldBe
                    listOf("thirdparty.alpha", "thirdparty.beta", "thirdparty.alpha", "thirdparty.alpha")
            }

            "plugins and the old service contracts are loaded together" {
                val legacy =
                    ServiceProviderClassLoader(
                        parent = PluginFixtures::class.java.classLoader,
                        registrations =
                            mapOf(
                                DomainResolver::class to listOf(FirstLabelResolver::class),
                                ArtifactContributor::class to listOf(LabelFileContributor::class),
                            ),
                    )
                val model =
                    MicrosmithBuilder()
                        .apply {
                            put(LabelExtension::class, LabelExtension)
                            put(FixtureExtension::class, FixtureExtension("hello"))
                        }
                        .model
                val output = newOutputDirectory()

                testGenerator(RecordingDiagnosticSink())
                    .generateTo(model, output, PluginFixtures.classLoader("alpha", parent = legacy))

                generatedTexts(output).keys shouldBe setOf("alpha.txt", "label.txt", "label.bin")
            }
        },
    )
