package io.github.lmliam.microsmith.cli.plugins

import io.github.lmliam.microsmith.cli.command.RunCommand
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import java.io.IOException
import java.io.UncheckedIOException
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverGuardrailTests :
    StringSpec({
        "returns success without evaluating credential resolvers when no plugins are requested" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-resolver-no-plugins")
            try {
                val command =
                    RunCommand(
                        script = tempDir.resolve("schema.microsmith.kts"),
                        outputDir = tempDir.resolve("generated"),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials {
                                error("resolve() should not be called when no plugins are requested.")
                            }

                            override fun sensitiveValues(): Set<String> {
                                error("sensitiveValues() should not be called when no plugins are requested.")
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath.shouldBeEmpty()
                success.lockfilePath shouldBe null
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "resolves local plugin jars without evaluating repository credential resolvers" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-resolver-local-only")
            try {
                val script = tempDir.resolve("schema.microsmith.kts")
                script.writeText("// test script")
                val localJar = tempDir.resolve("plugins/local-plugin.jar")
                localJar.parent.createDirectories()
                localJar.writeBytes("local-plugin".toByteArray())

                val relativeJarPath = relativizeFromWorkingDirectory(localJar)
                val command =
                    RunCommand(
                        script = script,
                        outputDir = tempDir.resolve("generated"),
                        pluginJars = setOf(relativeJarPath),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials {
                                error("resolve() should not be called for local plugin-jar-only runs.")
                            }

                            override fun sensitiveValues(): Set<String> {
                                error("sensitiveValues() should not be called for local plugin-jar-only runs.")
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                success.classpath shouldBe listOf(localJar.toAbsolutePath().normalize())
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "returns failure diagnostics when credential sensitive values cannot be initialized" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-resolver-credential-init")
            try {
                val command =
                    RunCommand(
                        script = tempDir.resolve("schema.microsmith.kts"),
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:plugin:1.0.0"),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials? {
                                error("resolve() should not be called when sensitive value initialization fails.")
                            }

                            override fun sensitiveValues(): Set<String> {
                                throw IllegalArgumentException("Repository credentials file is invalid.")
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                failure.diagnostics.joinToString("\n").shouldBe(
                    "[authentication] Repository credentials file is invalid.",
                )
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "returns authentication diagnostics when credential initialization cannot read backing state" {
            val tempDir = createWorkingDirectoryTempDirectory("microsmith-plugin-resolver-credential-io")
            try {
                val command =
                    RunCommand(
                        script = tempDir.resolve("schema.microsmith.kts"),
                        outputDir = tempDir.resolve("generated"),
                        plugins = setOf("com.acme:plugin:1.0.0"),
                    )
                val settings =
                    PluginResolverSettings(
                        cacheDirectory = tempDir.resolve("cache"),
                        repositoryCredentialsResolver =
                        object : RepositoryCredentialsResolver {
                            override fun resolve(repositoryUri: String): RepositoryCredentials? {
                                error("resolve() should not be called when sensitive value initialization fails.")
                            }

                            override fun sensitiveValues(): Set<String> {
                                throw UncheckedIOException(
                                    IOException("Repository credentials file could not be read."),
                                )
                            }
                        },
                    )

                val result = resolvePlugins(command = command, settings = settings)
                val failure = result.shouldBeTypeOf<PluginResolutionResult.Failure>()
                val message = failure.diagnostics.joinToString("\n")
                message.shouldContain("[authentication]")
                message.shouldContain("Repository credentials file could not be read.")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
